import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/progress-dashboard/api',
})

export default api