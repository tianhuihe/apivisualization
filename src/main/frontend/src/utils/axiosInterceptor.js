/**
 * Axios请求拦截器
 * 统一处理HTTP请求错误和响应
 */
import axios from 'axios';
import { ElMessage } from 'element-plus';

// 创建axios实例
const service = axios.create({
  baseURL: '/api',
  timeout: 5000
});

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 可以在这里统一添加请求头
    return config;
  },
  error => {
    // 请求错误处理
    console.error('请求错误:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器
service.interceptors.response.use(
  response => {
    // 对响应数据做处理
    return response.data;
  },
  error => {
    // 统一错误处理
    let errorMessage = '请求失败';
    
    if (error.response) {
      // 服务器返回错误状态码
      switch (error.response.status) {
        case 400:
          errorMessage = '请求参数错误';
          break;
        case 401:
          errorMessage = '未授权，请登录';
          break;
        case 403:
          errorMessage = '拒绝访问';
          break;
        case 404:
          errorMessage = `请求地址不存在: ${error.config.url}`;
          break;
        case 500:
          errorMessage = '服务器内部错误';
          break;
        case 503:
          errorMessage = '服务不可用';
          break;
        default:
          errorMessage = `请求错误: ${error.response.status}`;
      }
    } else if (error.request) {
      // 请求已发出但没有响应
      errorMessage = '网络连接异常，请检查网络';
    } else {
      // 其他错误
      errorMessage = error.message;
    }
    
    // 显示错误提示
    ElMessage.error(errorMessage);
    
    return Promise.reject(error);
  }
);

export default service;