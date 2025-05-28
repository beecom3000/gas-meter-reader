import { ref } from 'vue';
import { io, Socket } from 'socket.io-client';

export const useSocketIo = () => {

  const socket = ref<Socket | null>(null);

  // Initialize Socket.IO connection
  const initSocket = async (hostname: string = 'localhost', port: number = 9092, namespace: string = '', secure: boolean = false) => {

    const protocol = secure ? 'https' : 'http';

    const url = `${protocol}://${hostname}:${port}/${namespace}`;

    console.log(`[Socket.io] Connecting to ${url}`);

    // Connect to Socket.IO Server
    // Add these options when creating the socket
    socket.value = io(url, {
      transports: ['websocket'],
      forceBase64: false, // Force binary transmission
      reconnection: true,
      // parser: {
      //   decodeResponse: false, // Prevent auto-parsing
      // }
      // perMessageDeflate: {
      //   threshold: 1024, // Only compress if payload > 1KB
      //   // level: 6         // Compression level (1-9)
      // }
    });
    socket.value.on('connect', () => {
      console.log('[Socket.io] Connected to server');
    });
    socket.value.on('disconnect', () => {
      console.log('[Socket.io] Disconnected from server');
    });
  }

  const terminateSocket = () => {
    if (socket.value) {
      socket.value.disconnect();
      socket.value = null;
    }
  }

  return {
    socket,
    initSocket,
    terminateSocket
  }
}
