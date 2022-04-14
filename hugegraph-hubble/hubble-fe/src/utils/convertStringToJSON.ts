export default function convertStringToJSON(text: any): object | null {
  if (typeof text !== 'string') {
    return null;
  }

  let context;

  try {
    context = JSON.parse(text);
  } catch (e) {
    return null;
  }

  return typeof context === 'object' && context !== null ? context : null;
}
