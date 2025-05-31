import './assets/main.css'

import { createApp } from 'vue'
import { Quasar } from 'quasar'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

// Material Icon libraries
import '@quasar/extras/material-icons/material-icons.css'

// Quasar css
import 'quasar/src/css/index.sass'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.use(Quasar, {
  plugins: {}, // import Quasar plugins and add here
})

app.mount('#app')
