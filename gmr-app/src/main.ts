import './assets/main.css'

import { createApp, type App as VueApp } from 'vue'
import { Dialog, Notify, Quasar } from 'quasar'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

// Material Icon libraries
import '@quasar/extras/material-icons/material-icons.css'

// Quasar css
import 'quasar/src/css/index.sass'
import mitt, { type Emitter } from 'mitt'
import { emitterKey, type Events } from '@/models/event-bus.ts'

const emitter: Emitter<Events> = mitt<Events>()
const app: VueApp<Element> = createApp(App)

app.use(createPinia())
app.use(router)
app.provide(emitterKey, emitter)

app.use(Quasar, {
  plugins: {
    Dialog,
    Notify
  }, // import Quasar plugins and add here
  // config: {
  //   notify
  // }
})

app.mount('#app')
