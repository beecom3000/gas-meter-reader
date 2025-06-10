<template>
  <div class="status-bar">
<!--    <div class="status-indicator">-->
<!--      <div class="status-dot" :class="{ 'offline': !isConnected }"></div>-->
<!--      <span>{{ connectionStatus }}</span>-->
<!--    </div>-->

    <q-item
      clickable
      v-ripple
      class="rounded-borders status-bar"
      :class="$q.dark.isActive ? 'bg-grey-9 text-white' : 'bg-grey-2'"
    >
      <q-item-section avatar>
        <q-avatar rounded>
          <q-icon
            name="circle"
            :color="isConnected ? 'green' : 'red'"
            size="20px"
          ></q-icon>
        </q-avatar>
      </q-item-section>

      <q-item-section>
        <q-item-label>
          {{ connectionStatus }}
        </q-item-label>
      </q-item-section>

      <q-item-section side>
        <span>Last update: {{ lastUpdate }}</span>
      </q-item-section>
    </q-item>
  </div>

</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
// import type { Emitter } from 'mitt'
// import { emitterKey, type Events } from '@/models/event-bus.ts'

const lastUpdate = ref<string>('Never')
// const emitter: Emitter<Events> = inject(emitterKey);
const props = defineProps<{
  isConnected: boolean
}>()

// emitter.on('socket-status-changed', ())

const connectionStatus = computed(() => {
  return props.isConnected ? 'Connected to backend' : 'Backend connection lost';
});
</script>

<style scoped>
.status-bar {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 15px;
  padding: 5px;
  display: flex;
  width: 100%;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 5px 15px rgba(0,0,0,0.1);
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-dot {
  width: 15px;
  height: 15px;
  border-radius: 50%;
  background: var(--success);
}

.status-dot.offline {
  background: var(--danger);
}

</style>
