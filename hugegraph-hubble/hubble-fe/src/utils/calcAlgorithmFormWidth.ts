export function calcAlgorithmFormWidth(
  expand: boolean,
  min: number,
  max: number
) {
  return expand ? min : max;
}
