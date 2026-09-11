import { fetchEventSource } from '@microsoft/fetch-event-source';

export const streamChat = async ({
  url,
  token,
  payload,
  signal,
  onMessage,
  onError,
  onClose
}) => {
  await fetchEventSource(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { _token: token } : {})
    },
    body: JSON.stringify(payload),
    signal,
    openWhenHidden: true,
    async onopen(response) {
      if (response.status === 401) {
        const error = new Error('LOGIN_EXPIRED');
        error.status = 401;
        throw error;
      }
      if (!response.ok) {
        const error = new Error(`HTTP error! status: ${response.status}`);
        error.status = response.status;
        throw error;
      }
    },
    onmessage(event) {
      if (!event.data) return;
      try {
        onMessage?.(JSON.parse(event.data));
      } catch (error) {
        onError?.(error, event.data);
      }
    },
    onclose() {
      onClose?.();
    },
    onerror(error) {
      onError?.(error);
      throw error;
    }
  });
};
