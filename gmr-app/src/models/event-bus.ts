import type { InjectionKey } from 'vue'
import type { Emitter } from 'mitt'

export type Events = {
  'last-update': { timestamp: Date };
  'status-change': { status: string };
};

export const emitterKey: InjectionKey<Emitter<Events>> = Symbol('emitter');
