import Vue from 'vue'

import Cookies from 'js-cookie'

import Element from 'element-ui'
import './assets/styles/element-variables.scss'

import '@/assets/styles/index.scss' // global css
import '@/assets/styles/ruoyi.scss' // ruoyi css

/*k1*/
//import VueCesium from 'vue-cesium'
//import 'vue-cesium/lib/theme-default/index.css'   // v1.x 的样式文件
//import * as Cesium from 'cesium/Build/Cesium/Cesium';
//import 'cesium/Build/Cesium/Widgets/widgets.css';
//window.Cesium = Cesium;

import App from './App'
import store from './store'
import router from './router'
import directive from './directive' // directive
import plugins from './plugins' // plugins
import { download } from '@/utils/request'

import './assets/icons' // icon
import './permission' // permission control
import { getDicts } from "@/api/system/dict/data"
import { getConfigKey } from "@/api/system/config"
import { parseTime, resetForm, addDateRange, selectDictLabel, selectDictLabels, handleTree } from "@/utils/ruoyi"
// 分页组件
import Pagination from "@/components/Pagination"
// 自定义表格工具组件
import RightToolbar from "@/components/RightToolbar"
// 富文本组件
import Editor from "@/components/Editor"
// 文件上传组件
import FileUpload from "@/components/FileUpload"
// 图片上传组件
import ImageUpload from "@/components/ImageUpload"
// 图片预览组件
import ImagePreview from "@/components/ImagePreview"
// 字典标签组件
import DictTag from '@/components/DictTag'
// 字典数据组件
import DictData from '@/components/DictData'

// 全局方法挂载
Vue.prototype.getDicts = getDicts
Vue.prototype.getConfigKey = getConfigKey
Vue.prototype.parseTime = parseTime
Vue.prototype.resetForm = resetForm
Vue.prototype.addDateRange = addDateRange
Vue.prototype.selectDictLabel = selectDictLabel
Vue.prototype.selectDictLabels = selectDictLabels
Vue.prototype.download = download
Vue.prototype.handleTree = handleTree

// 全局组件挂载
Vue.component('DictTag', DictTag)
Vue.component('Pagination', Pagination)
Vue.component('RightToolbar', RightToolbar)
Vue.component('Editor', Editor)
Vue.component('FileUpload', FileUpload)
Vue.component('ImageUpload', ImageUpload)
Vue.component('ImagePreview', ImagePreview)

Vue.use(directive)
Vue.use(plugins)
DictData.install()

/**
 * If you don't want to use mock-server
 * you want to use MockJs for mock api
 * you can execute: mockXHR()
 *
 * Currently MockJs will be used in the production environment,
 * please remove it before going online! ! !
 */

Vue.use(Element, {
  size: Cookies.get('size') || 'medium' // set element-ui default size
})

//Vue.use(VueCesium)//k1
// 配置 Cesium Ion Token（从 https://cesium.com/ion/ 免费获取）
if (window.Cesium) {
  // 替换为你自己的 Token，或使用下面的公开测试 Token（可能有限制）
  window.Cesium.Ion.defaultAccessToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiIyYzI0NmQzMy1jN2U0LTRhYzAtYjE2OC1mZTBhM2I0ODQ1NzkiLCJpZCI6MjU5LCJpYXQiOjE2NDI4NzkwMjZ9.5UZ8uZqBv5xjhY7vQn7g9Vq7Y5Q9v5uLp5Z4V8J7W1E'
}

Vue.config.productionTip = false

new Vue({
  el: '#app',
  router,
  store,
  render: h => h(App)
})
