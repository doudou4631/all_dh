/** Plan A stub: full TDX package includes Tencent routes; secondary-appeal-only does not. */
export async function processTencent3hCleanup() {
  return {
    ok: false,
    message: '方案 A 未包含腾讯清理通道，请使用完整 TDX 模块或二次申诉接口。',
  }
}

export async function processTencent23dCleanup() {
  return {
    ok: false,
    message: '方案 A 未包含腾讯清理通道，请使用完整 TDX 模块或二次申诉接口。',
  }
}
