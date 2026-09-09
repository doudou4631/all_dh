const http = require('http');

const PROXY_PORT = 18888;
const PROXY_HOST = '127.0.0.1';

let cachedProxy = null;
let lastFetchTime = 0;
const CACHE_DURATION_MS = 45 * 1000; // Cache for 45 seconds
let isFetching = false;
let fetchPromise = null;

async function getProxy(forceRefresh = false) {
  const now = Date.now();
  if (!forceRefresh && cachedProxy && (now - lastFetchTime < CACHE_DURATION_MS)) {
    return cachedProxy;
  }
  if (isFetching) {
    return fetchPromise;
  }
  isFetching = true;
  fetchPromise = new Promise((resolve, reject) => {
    const proxySourceUrl = process.env.QMF_PROXY_SOURCE_URL || 'YOUR_QMF_PROXY_SOURCE_URL';
    console.log("Fetching new proxy from ydaili...");
    http.get(proxySourceUrl, (res) => {
      let data = '';
      res.on('data', (chunk) => {
        data += chunk;
      });
      res.on('end', () => {
        isFetching = false;
        const result = data.trim();
        if (result && /^\d+\.\d+\.\d+\.\d+:\d+$/.test(result)) {
          cachedProxy = result;
          lastFetchTime = Date.now();
          console.log(`Fetched fresh proxy: ${cachedProxy}`);
          resolve(cachedProxy);
        } else {
          console.error(`Invalid proxy response: ${result}`);
          if (cachedProxy) {
            console.log(`Fallback to cached proxy: ${cachedProxy}`);
            resolve(cachedProxy);
          } else {
            reject(new Error(`Invalid proxy: ${result}`));
          }
        }
      });
    }).on('error', (err) => {
      isFetching = false;
      console.error(`Proxy fetch error: ${err.message}`);
      if (cachedProxy) {
        resolve(cachedProxy);
      } else {
        reject(err);
      }
    });
  });
  return fetchPromise;
}

const server = http.createServer(async (req, res) => {
  console.log(`Received proxy request: ${req.method} ${req.url}`);
  
  // Buffer the request body so we can retry if needed
  let bodyChunks = [];
  req.on('data', (chunk) => {
    bodyChunks.push(chunk);
  });
  
  req.on('end', async () => {
    const bodyBuffer = Buffer.concat(bodyChunks);
    let attempts = 0;
    const maxAttempts = 2;
    let success = false;
    
    while (attempts < maxAttempts && !success) {
      attempts++;
      let proxyStr = null;
      try {
        proxyStr = await getProxy(attempts > 1);
      } catch (err) {
        res.writeHead(502, { 'Content-Type': 'text/plain' });
        res.end(`Proxy Error: Unable to fetch upstream proxy: ${err.message}`);
        return;
      }
      
      const [proxyHost, proxyPortStr] = proxyStr.split(':');
      const proxyPort = parseInt(proxyPortStr, 10);
      
      console.log(`Attempt ${attempts}: Forwarding to proxy ${proxyHost}:${proxyPort} for ${req.url}`);
      
      const outgoingHeaders = {};
      for (const key in req.headers) {
        if (key.toLowerCase() !== 'proxy-connection') {
          outgoingHeaders[key] = req.headers[key];
        }
      }
      
      const proxyReq = http.request({
        hostname: proxyHost,
        port: proxyPort,
        method: req.method,
        path: req.url, // Pass full absolute URL
        headers: outgoingHeaders,
        timeout: 8000 // 8 second timeout per attempt
      });
      
      const responsePromise = new Promise((resolve) => {
        proxyReq.on('response', (proxyRes) => {
          res.writeHead(proxyRes.statusCode, proxyRes.headers);
          proxyRes.pipe(res);
          success = true;
          resolve(true);
        });
        
        proxyReq.on('error', (err) => {
          console.error(`Proxy request error (attempt ${attempts}): ${err.message}`);
          cachedProxy = null; // Invalidate cache so next attempt fetches fresh proxy
          resolve(false);
        });
        
        proxyReq.on('timeout', () => {
          console.error(`Proxy request timeout (attempt ${attempts})`);
          proxyReq.destroy();
          cachedProxy = null; // Invalidate cache
          resolve(false);
        });
      });
      
      if (bodyBuffer.length > 0) {
        proxyReq.write(bodyBuffer);
      }
      proxyReq.end();
      
      const ok = await responsePromise;
      if (ok) {
        break;
      }
    }
    
    if (!success) {
      res.writeHead(504, { 'Content-Type': 'text/plain' });
      res.end('Gateway Timeout: Connection to upstream proxy failed after retries.');
    }
  });
});

server.listen(PROXY_PORT, PROXY_HOST, () => {
  console.log(`Proxy server listening on http://${PROXY_HOST}:${PROXY_PORT}`);
});
