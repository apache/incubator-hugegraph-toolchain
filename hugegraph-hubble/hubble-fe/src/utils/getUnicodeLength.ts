export default function getUnicodeLength(str?: string) {
  if (typeof str === 'undefined') {
    return 0;
  }

  return str.replace(/[^\x00-\xff]/g, '01').length;
}
