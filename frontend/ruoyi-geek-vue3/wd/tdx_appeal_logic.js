const TDX_SEND_SMS_URL = 'https://customer-apply-diversion.teddymobile.cn/api/v1/as/auth/sendSms';
const TDX_LOGIN_SMS_URL = 'https://customer-apply-diversion.teddymobile.cn/api/v1/auth/login-sms';
const TDX_ANSWERS_START_URL = 'https://customer-apply-diversion.teddymobile.cn/api/v1/answers/start';
const TDX_CHECK_PHONE_URL = 'https://customer-apply-diversion.teddymobile.cn/api/v1/forward/inter/checkAppletPhone';
const TDX_ANSWERS_SUBMIT_URL = 'https://customer-apply-diversion.teddymobile.cn/api/v1/answers/submit-and-next';
const TDX_QUERY_PROGRESS_URL = 'https://customer-apply-diversion.teddymobile.cn/api/v1/forward/inter/queryAppletProgressList';

const MINI_APP_REFERER = 'https://servicewechat.com/wxf123c0cb425589de/7/page-frame.html';
const MINI_APP_USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 MicroMessenger/7.0.20.1781(0x6700143B) NetType/WIFI MiniProgramEnv/Windows WindowsWechat/WMPF WindowsWechat(0x63090a13) UnifiedPCWindowsWechat(0xf254173b) XWEB/19027';

const DEFAULT_ANSWERS = [
  { questionCode: '1-1', selectedOptionCode: '1-1-A', answerText: '本人使用手机号接听来电。' },
  { questionCode: '1-2', selectedOptionCode: '1-2-A', answerText: '近期频繁收到标记来电，影响正常通信。' },
  { questionCode: '1-3', selectedOptionCode: '1-3-A', answerText: '近5天内持续出现。' },
  { questionCode: '1-4', selectedOptionCode: '1-4-A', answerText: '每天约1次以上。' },
  { questionCode: '1-5', selectedOptionCode: '1-5-A', answerText: '该号码为本人实名号码，未从事营销行为。' },
  { questionCode: '1-6', selectedOptionCode: '1-6-A', answerText: '?' },
  { questionCode: '1-16', selectedOptionCode: '1-16-B', answerText: '仅供个人正常通信使用。' },
  { questionCode: '1-15', selectedOptionCode: '1-15-A', answerText: '请协助核实并尽快清理标记。' },
];

const ONE_PIXEL_IMAGE_BASE64 = '/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=';
const ONE_PIXEL_IMAGE_BUF = Buffer.from(ONE_PIXEL_IMAGE_BASE64, 'base64');

function getMiniAppHeaders(token = '', contentType = 'application/json') {
  const headers = {
    Accept: '*/*',
    'User-Agent': MINI_APP_USER_AGENT,
    xweb_xhr: '1',
    'Sec-Fetch-Site': 'cross-site',
    'Sec-Fetch-Mode': 'cors',
    'Sec-Fetch-Dest': 'empty',
    Referer: MINI_APP_REFERER,
    'Accept-Encoding': 'gzip, deflate, br',
    'Accept-Language': 'zh-CN,zh;q=0.9',
  };
  if (contentType) headers['Content-Type'] = contentType;
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return headers;
}

export async function sendTdxSmsCode(phone) {
  const res = await fetch(TDX_SEND_SMS_URL, {
    method: 'POST',
    headers: getMiniAppHeaders('', 'application/json'),
    body: JSON.stringify({ phone, bizType: 'LOGIN' })
  });
  const data = await res.json();
  return data;
}

export async function fetchWxCodeFromQmf(qmfConfig, orderId = '') {
  const loginRes = await fetch(qmfConfig.loginUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      username: qmfConfig.username,
      password: qmfConfig.password
    })
  });
  const loginData = await loginRes.json();
  const qmfToken = loginData?.data?.usertoken || loginData?.token;
  if (!qmfToken) {
    throw new Error('Unable to acquire QMF token');
  }

  const codeRes = await fetch(qmfConfig.codeUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      token: qmfToken,
      appId: qmfConfig.appId || '78811',
      qrCode: '1',
      orderId
    })
  });
  const codeData = await codeRes.json();
  if (codeData?.code !== 20000 && codeData?.code !== 200) {
    throw new Error(`QMF API returned error: ${codeData.msg || 'unknown error'}`);
  }

  return {
    orderId: codeData?.data?.orderId || orderId,
    wxCode: codeData?.data?.wxCode
  };
}

export async function loginTdxWithSms(phone, smsCode, openId) {
  const res = await fetch(TDX_LOGIN_SMS_URL, {
    method: 'POST',
    headers: getMiniAppHeaders('', 'application/json'),
    body: JSON.stringify({
      phone,
      smsCode,
      nickname: '',
      openId,
      originalIp: '127.0.0.1'
    })
  });
  const data = await res.json();
  const token = data?.token || data?.data?.token;
  if (!token) {
    throw new Error(`TDX Login failed: ${data.message || 'no token returned'}`);
  }
  return token;
}

export async function executeTdxAppeal(phone, loginToken, openId) {
  const startRes = await fetch(TDX_ANSWERS_START_URL, {
    method: 'POST',
    headers: getMiniAppHeaders(loginToken, 'application/json'),
    body: JSON.stringify({ questionnaireId: 1 })
  });
  const startData = await startRes.json();
  const sessionId = startData?.data?.sessionId || startData?.sessionId;
  if (!sessionId) {
    throw new Error(`Failed to start questionnaire session: ${startData?.message || 'no sessionId'}`);
  }

  const checkRes = await fetch(TDX_CHECK_PHONE_URL, {
    method: 'POST',
    headers: getMiniAppHeaders(loginToken, 'application/json'),
    body: JSON.stringify({ phone, openId, originalIp: '127.0.0.1' })
  });
  const checkData = await checkRes.json();
  if (checkData?.code !== 200 && checkData?.success !== true) {
    throw new Error(`Phone binding check failed: ${checkData?.message}`);
  }

  for (const answer of DEFAULT_ANSWERS.slice(0, 6)) {
    const ansRes = await fetch(TDX_ANSWERS_SUBMIT_URL, {
      method: 'POST',
      headers: getMiniAppHeaders(loginToken, 'application/json'),
      body: JSON.stringify({
        sessionId,
        selectedOptionCode: answer.selectedOptionCode,
        answerText: answer.answerText,
        questionCode: answer.questionCode
      })
    });
    const ansData = await ansRes.json();
    if (ansData?.code !== 200 && ansData?.success !== true) {
      throw new Error(`Answer ${answer.questionCode} submit failed: ${ansData?.message}`);
    }
  }

  const form = new FormData();
  form.append('sessionId', String(sessionId));
  form.append('selectedOptionCode', '1-10-A');
  form.append('questionCode', '1-10');
  const fileBlob = new Blob([ONE_PIXEL_IMAGE_BUF], { type: 'image/jpeg' });
  form.append('files', fileBlob, 'kb.jpg');

  const uploadRes = await fetch(TDX_ANSWERS_SUBMIT_URL, {
    method: 'POST',
    headers: getMiniAppHeaders(loginToken, null),
    body: form
  });
  const uploadData = await uploadRes.json();
  if (uploadData?.code !== 200 && uploadData?.success !== true) {
    throw new Error(`Evidence image upload failed: ${uploadData?.message}`);
  }

  for (const answer of DEFAULT_ANSWERS.slice(6)) {
    const ansRes = await fetch(TDX_ANSWERS_SUBMIT_URL, {
      method: 'POST',
      headers: getMiniAppHeaders(loginToken, 'application/json'),
      body: JSON.stringify({
        sessionId,
        selectedOptionCode: answer.selectedOptionCode,
        answerText: answer.answerText,
        questionCode: answer.questionCode
      })
    });
    const ansData = await ansRes.json();
    if (ansData?.code !== 200 && ansData?.success !== true) {
      throw new Error(`Answer ${answer.questionCode} submit failed: ${ansData?.message}`);
    }
  }

  const progressRes = await fetch(TDX_QUERY_PROGRESS_URL, {
    method: 'POST',
    headers: getMiniAppHeaders(loginToken, 'application/json'),
    body: JSON.stringify({})
  });
  const progressData = await progressRes.json();
  return {
    success: true,
    sessionId,
    progressData
  };
}
