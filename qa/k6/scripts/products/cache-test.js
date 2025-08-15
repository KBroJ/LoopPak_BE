import http from 'k6/http';
import { check, sleep } from 'k6';

// 테스트 옵션: 5명의 가상 유저가 30초 동안 테스트를 실행
export const options = {
  vus: 5,           // 가상 유저 수 : 5명
  duration: '30s',  // 테스트 실행 시간 : 30초
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PRODUCT_ID = 1; // 테스트할 상품 ID
const BRAND_ID = 1;   // 테스트할 브랜드 ID

export default function () {
  // 1. 상품 상세 조회 API 호출
  const res1 = http.get(`${BASE_URL}/api/v1/products/${PRODUCT_ID}`);
  check(res1, {
    'product detail success': (r) => r.status === 200,
  });

  sleep(1); // 1초 대기

  // 2. 상품 목록 조회 (좋아요 순) API 호출
  const res2 = http.get(`${BASE_URL}/api/v1/products?brandId=${BRAND_ID}&sort=likes_desc&page=0&size=20`);
  check(res2, {
    'product list success': (r) => r.status === 200,
  });

  sleep(1);
}