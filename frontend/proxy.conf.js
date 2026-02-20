// configuration for serving API requests locally during development

const PROXY_CONFIG = [
  {
    context: ['/api'],
    target: 'http://localhost:8080',
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
    headers: {
      'X-Custom-Header': 'true'
    }
  }
];

module.exports = PROXY_CONFIG;
