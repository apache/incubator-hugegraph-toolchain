export default function isDataTypeNumeric(type: string | undefined) {
  return (
    type === 'BYTE' ||
    type === 'INT' ||
    type === 'LONG' ||
    type === 'FLOAT' ||
    type === 'DOUBLE' ||
    type === 'DATE'
  );
}
