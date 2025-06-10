import type { InjectionKey } from 'vue'
import type { Emitter } from 'mitt'

export type Events = {
  message: string;
  status: string;
};

export const emitterKey: InjectionKey<Emitter<Events>> = Symbol('emitter');
