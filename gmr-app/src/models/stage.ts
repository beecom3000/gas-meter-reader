export enum Stage {
  grayscale = 'Grayscale',
  blurred = 'Blurred',
  canny_edge = 'Canny Edge',
  threshold = 'Threshold',
  final = 'Final'
}

export const toStage = (str: string): Stage | undefined => {
  // Use 'as keyof typeof Stage' to assert that 'str' is a potential key of the Stage enum.
  // This allows accessing the enum using bracket notation.
  const enumValue = Stage[str as keyof typeof Stage];

  // Check if the retrieved value is actually a valid enum member
  // by ensuring it's one of the enum's string values.
  if (Object.values(Stage).includes(enumValue)) {
    return enumValue;
  }
  return undefined; // Return undefined if the string is not a valid enum member
}
