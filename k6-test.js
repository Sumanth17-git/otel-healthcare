import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const loginDuration = new Trend('login_duration');

export const options = {
  stages: [
    { duration: '15s', target: 5 },   // ramp up to 5 users
    { duration: '1800s', target: 10 },  // hold at 10 users
    { duration: '10s', target: 0 },   // ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],  // 95% of requests under 2s
    errors: ['rate<0.1'],              // error rate under 10%
  },
};

const BASE_URL = 'http://34.9.131.147:8080';

// Test data
const validPatient = {
  patientId: 'patient001',
  password: 'pass123',
  accessPin: '4321',
};

const blockedStaff = {
  patientId: 'staff001',
  password: 'admin123',
  accessPin: '9999',
};

const invalidPatient = {
  patientId: 'unknown',
  password: 'wrongpass',
  accessPin: '0000',
};

function log(label, res) {
  console.log(`[${label}] status=${res.status} duration=${res.timings.duration.toFixed(2)}ms body=${res.body}`);
}

export default function () {
  group('Healthcare Auth - Valid Login', function () {
    const res = http.post(
      `${BASE_URL}/auth/login`,
      JSON.stringify(validPatient),
      { headers: { 'Content-Type': 'application/json' } }
    );

    log('Valid Login', res);

    const success = check(res, {
      'valid login: status 200': (r) => r.status === 200,
      'valid login: returns token': (r) => r.body && r.body.length > 0,
    });

    loginDuration.add(res.timings.duration);
    errorRate.add(!success);
    sleep(1);
  });

  group('Healthcare Auth - Blocked User', function () {
    const res = http.post(
      `${BASE_URL}/auth/login`,
      JSON.stringify(blockedStaff),
      { headers: { 'Content-Type': 'application/json' } }
    );

    log('Blocked User', res);

    check(res, {
      'blocked user: status 500': (r) => r.status === 500,
    });

    sleep(1);
  });

  group('Healthcare Auth - Invalid Credentials', function () {
    const res = http.post(
      `${BASE_URL}/auth/login`,
      JSON.stringify(invalidPatient),
      { headers: { 'Content-Type': 'application/json' } }
    );

    log('Invalid Credentials', res);

    check(res, {
      'invalid credentials: status 500': (r) => r.status === 500,
    });

    sleep(1);
  });

  group('Gateway Health Check', function () {
    const res = http.get(`${BASE_URL}/health`);

    log('Health Check', res);

    check(res, {
      'gateway health: status 200': (r) => r.status === 200,
    });
    sleep(0.5);
  });
}
