package com.loopers.domain.users;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Entity
@Table(name = "users")
public class UserModel extends BaseEntity {

    @NotNull
    @Column(name = "user_id")
    private String userId;
    @NotNull
    private String gender;
    @NotNull
    private LocalDate birthDate;
    @NotNull
    private String email;

    public String getUserId() {
        return userId;
    }
    public String getGender() {
        return gender;
    }
    public LocalDate getBirthDate() {
        return birthDate;
    }
    public String getEmail() {
        return email;
    }

    protected UserModel() {}

    public UserModel(String userId, String gender, String birthDateStr, String email) {

        validateUserId(userId);
        validateGender(gender);
        validateBirthDate(birthDateStr);
        validateEmail(email);

    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "아이디는 비어있을 수 없습니다.");
        }
        if (!userId.matches("^[a-zA-Z0-9]{1,10}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID는 영문 및 숫자로 10자 이내여야 합니다.");
        }

        this.userId = userId;
    }

    private void validateGender(String gender) {
        if (gender == null || gender.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "성별은 비어있을 수 없습니다.");
        }

        this.gender = gender;
    }

    private void validateBirthDate(String birthDateStr) {
        if (birthDateStr == null || birthDateStr.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.");
        }
        try {
            this.birthDate = LocalDate.parse(birthDateStr);
        } catch (DateTimeParseException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일 형식이 올바르지 않습니다.(yyy-MM-dd)");
        }

    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.");
        }
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.");
        }

        this.email = email;
    }

    public static UserModel of(String userId, String gender, String birthDateStr,String email) {
        return new UserModel(userId, gender, birthDateStr, email);
    }

}
