import { fileURLToPath, URL } from 'node:url'
import { defineConfig ,loadEnv} from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
// import compression from 'vite-plugin-compression';

export default defineConfig(({ mode }) => {
  // 加载环境变量
  const env = loadEnv(mode, process.cwd(), '')

  return {
    
    plugins: [
      vue(),
      AutoImport({
        resolvers: [ElementPlusResolver()],
      }),
      Components({
        resolvers: [ElementPlusResolver()],
      }),
      // compression({
      //   verbose: true,         // 输出压缩结果
      //   disable: false,        // 是否禁用
      //   threshold: 10240,      // 体积大于该值时才会压缩(10KB)
      //   algorithm: 'gzip',     // 压缩算法：gzip, brotliCompress, deflate, deflateRaw
      //   ext: '.gz',            // 生成的压缩文件扩展名
      //   deleteOriginFile: true, // 是否删除原文件
      // }),
    ],
    server: {
      allowedHosts:true,
      host: '0.0.0.0', // 监听所有地址
      port: 3000, 
      proxy: {
        'http://192.168.3.8:3000/production': {  // 根据环境变量动态设置代理目标
          target: 'http://YOUR_API_HOST:8084/api', // 从环境变量加载或使用默认值
          changeOrigin: true, // 允许跨域请求时改变原始主机头
          rewrite: (path) => path.replace(/^\/api/, '') // 重写路径，如果需要的话
        }
      }
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
    build: {
      minify: 'terser', // 使用terser压缩代码
      terserOptions: {
        compress: {
         // drop_console: true, // 移除console
          drop_debugger: true // 移除debugger
        }
      },
      rollupOptions: {
        output: {
          manualChunks(id) {
            // 分割代码，将较大的依赖单独打包
            if (id.includes('node_modules')) {
              return id.toString().split('node_modules/')[1].split('/')[0].toString();
            }
          }
        }
      }
    }
  }
})  