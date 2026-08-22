import http from 'node:http';
import { NotFoundError } from './store.mjs';

const startedAt = Date.now();

export function createApiServer({ store, enforcer, config }) {
  return http.createServer(async (request, response) => {
    try {
      const url = new URL(request.url, `http://${request.headers.host || 'localhost'}`);

      if (url.pathname === '/health') {
        return sendJson(response, 200, { ok: true });
      }

      if (!isAuthorized(request, config.adminToken)) {
        response.setHeader('WWW-Authenticate', 'Bearer');
        return sendJson(response, 401, { error: 'unauthorized' });
      }

      if (request.method === 'GET' && url.pathname === '/api/v1/status') {
        return sendJson(response, 200, {
          name: 'NetQuota Box',
          online: true,
          mode: config.mode,
          uptimeSeconds: Math.floor((Date.now() - startedAt) / 1000),
          wanOnline: true,
          version: '0.1.0'
        });
      }

      if (request.method === 'GET' && url.pathname === '/api/v1/devices') {
        return sendJson(response, 200, { devices: store.listDevices() });
      }

      if (request.method === 'GET' && url.pathname === '/api/v1/audit') {
        return sendJson(response, 200, { events: store.listAudit(Number(url.searchParams.get('limit') || 100)) });
      }

      const actionMatch = url.pathname.match(/^\/api\/v1\/devices\/([^/]+)\/(pause|resume|bonus)$/);
      if (request.method === 'POST' && actionMatch) {
        const deviceId = decodeURIComponent(actionMatch[1]);
        const action = actionMatch[2];
        let device;
        if (action === 'bonus') {
          const body = await readJson(request);
          device = store.addBonus(deviceId, body.bytes);
        } else {
          device = store.setManualPaused(deviceId, action === 'pause');
        }
        await enforcer.setBlocked(device.ipAddress, device.paused);
        return sendJson(response, 200, device);
      }

      const quotaMatch = url.pathname.match(/^\/api\/v1\/devices\/([^/]+)\/quota$/);
      if (request.method === 'PUT' && quotaMatch) {
        const body = await readJson(request);
        const device = store.setQuota(decodeURIComponent(quotaMatch[1]), body.limitBytes);
        await enforcer.setBlocked(device.ipAddress, device.paused);
        return sendJson(response, 200, device);
      }

      return sendJson(response, 404, { error: 'not_found' });
    } catch (error) {
      if (error instanceof NotFoundError) return sendJson(response, 404, { error: error.message });
      if (error instanceof RangeError || error instanceof TypeError || error instanceof SyntaxError) {
        return sendJson(response, 400, { error: error.message });
      }
      console.error(error);
      return sendJson(response, 500, { error: 'internal_error' });
    }
  });
}

function isAuthorized(request, expectedToken) {
  const value = request.headers.authorization || '';
  return value === `Bearer ${expectedToken}`;
}

async function readJson(request) {
  let body = '';
  for await (const chunk of request) {
    body += chunk;
    if (body.length > 16_384) throw new RangeError('Request body is too large');
  }
  if (!body) return {};
  return JSON.parse(body);
}

function sendJson(response, statusCode, payload) {
  const body = JSON.stringify(payload);
  response.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body),
    'Cache-Control': 'no-store'
  });
  response.end(body);
}
