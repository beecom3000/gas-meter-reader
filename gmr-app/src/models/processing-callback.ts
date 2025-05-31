import type { Metadata } from '@/models/metadata.ts'

export type ProcessingCallback = (metadata: Metadata, data: ArrayBuffer) => void;
