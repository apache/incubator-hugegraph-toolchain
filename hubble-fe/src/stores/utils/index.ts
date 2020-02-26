export function checkIfLocalNetworkOffline(error: any) {
  if (!error.response) {
    throw new Error('网络错误，请查看您的本地连接');
  }
}
