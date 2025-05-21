<template>
  <div class="process">
    <el-dialog v-model="dialogFormVisible" :title="formType === 'create' ? '新建流程' : '编辑流程'">
      <el-form :model="form">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" autocomplete="off" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogFormVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="nodeDialogVisible" :title="nodeForm.id ? '编辑节点' : '新建节点'">
      <el-form :model="nodeForm">
        <el-form-item label="名称" prop="name" :rules="[{ required: true, message: '请输入节点名称', trigger: 'blur' }]">
          <el-input v-model="nodeForm.name" autocomplete="off" />
        </el-form-item>
        <el-form-item label="类型" prop="type" :rules="[{ required: true, message: '请选择节点类型', trigger: 'change' }]">
          <el-select v-model="nodeForm.type" placeholder="请选择节点类型">
            <el-option label="开始节点" value="start" />
            <el-option label="结束节点" value="end" />
            <el-option label="任务节点" value="task" />
            <el-option label="网关节点" value="gateway" />
          </el-select>
        </el-form-item>
        <el-form-item label="配置" prop="config" :rules="[{ validator: validateNodeConfig, trigger: 'blur' }]">
          <template v-if="nodeForm.type === 'start'">
            <el-form-item label="初始参数" prop="config.initParams">
              <el-input v-model="nodeForm.config.initParams" placeholder="请输入初始参数，如: {\"userId\": 123}" />
            </el-form-item>
          </template>
          <template v-else-if="nodeForm.type === 'end'">
            <el-form-item label="结果处理" prop="config.resultHandler">
              <el-select v-model="nodeForm.config.resultHandler" placeholder="请选择结果处理方式">
                <el-option label="直接返回" value="return" />
                <el-option label="存储到数据库" value="store" />
              </el-select>
            </el-form-item>
          </template>
          <template v-else-if="nodeForm.type === 'task'">
            <el-form-item label="任务类型" prop="config.taskType">
              <el-select v-model="nodeForm.config.taskType" placeholder="请选择任务类型">
                <el-option label="HTTP请求" value="http" />
                <el-option label="数据库操作" value="db" />
                <el-option label="脚本执行" value="script" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="nodeForm.config.taskType === 'http'" label="请求URL" prop="config.url">
              <el-input v-model="nodeForm.config.url" placeholder="请输入请求URL" />
            </el-form-item>
          </template>
          <template v-else-if="nodeForm.type === 'gateway'">
            <el-form-item label="网关类型" prop="config.gatewayType">
              <el-select v-model="nodeForm.config.gatewayType" placeholder="请选择网关类型">
                <el-option label="并行网关" value="parallel" />
                <el-option label="排他网关" value="exclusive" />
              </el-select>
            </el-form-item>
          </template>
          <template v-else>
            <el-input v-model="nodeForm.config" type="textarea" placeholder="请输入JSON格式配置" />
          </template>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nodeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitNode">确定</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="edgeDialogVisible" title="连线条件配置">
      <el-form :model="edgeConditionForm">
        <el-form-item label="条件表达式" prop="condition" :rules="[{ required: true, message: '请输入连线条件', trigger: 'blur' }, { validator: validateCondition, trigger: 'blur' }]">
          <el-input v-model="edgeConditionForm.condition" type="textarea" placeholder="例如: ${amount} > 1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="edgeDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleDeleteEdge">删除连线</el-button>
        <el-button type="primary" @click="handleSubmitEdgeCondition">确定</el-button>
      </template>
    </el-dialog>
    
    <el-tabs v-model="activeTab">
      <el-tab-pane label="流程定义" name="definition">
        <h1>流程管理</h1>
        <el-table :data="processDefinitions" style="width: 100%">
      <el-table-column prop="id" label="ID" width="180" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="description" label="描述" />
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
        <el-button type="primary" @click="handleCreate" style="margin-top: 20px">新建流程</el-button>
      </el-tab-pane>
      <el-tab-pane label="节点管理" name="nodes">
        <h1>节点管理</h1>
        <el-button type="primary" @click="handleCreateNode" style="margin-bottom: 20px">新建节点</el-button>
        <el-table :data="processNodes" style="width: 100%">
          <el-table-column prop="id" label="ID" width="180" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="type" label="类型" />
          <el-table-column label="操作" width="180">
            <template #default="scope">
              <el-button size="small" @click="handleEditNode(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteNode(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="流程设计" name="design">
          <h1>流程设计器</h1>
          <el-button type="primary" @click="saveProcessDesign" style="margin-bottom: 20px">保存设计</el-button>
        <div class="designer-container" ref="designerContainer">
          <div class="node-palette">
            <div 
              v-for="node in nodeTypes" 
              :key="node.type"
              class="node-item"
              draggable="true"
              @dragstart="handleDragStart($event, node)"
            >
              {{ node.name }}
            </div>
          </div>
          <div class="designer-area" @drop="handleDrop" @dragover="handleDragOver">
          <div 
            v-for="node in nodes" 
            :key="node.id"
            class="node"
            :style="{ left: `${node.x}px`, top: `${node.y}px` }"
            @mousedown="startConnecting(node.id)"
            @mouseup="endConnecting(node.id)"
          >
            {{ node.name }}
          </div>
          <svg class="edges">
            <line 
              v-for="edge in edges" 
              :key="edge.id"
              :x1="getNodeX(edge.source)"
              :y1="getNodeY(edge.source)"
              :x2="getNodeX(edge.target)"
              :y2="getNodeY(edge.target)"
              stroke="#409EFF"
              stroke-width="2"
              @contextmenu="handleEdgeContextMenu(edge, $event)"
            />
          </svg>
        </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from '@/utils/axiosInterceptor'

export default {
  name: 'Process',
  setup() {
    const processDefinitions = ref([])
    const processNodes = ref([])
    const dialogFormVisible = ref(false)
    const nodeDialogVisible = ref(false)
    const formType = ref('create')
    const activeTab = ref('definition')
    const form = ref({ name: '', description: '' })
    const nodeForm = ref({ name: '', type: '', config: '' })
    const nodeTypes = ref([
       { type: 'start', name: '开始节点', config: { canConnectTo: ['task', 'decision'] } },
       { type: 'end', name: '结束节点', config: { canConnectFrom: ['task', 'decision'] } },
       { type: 'task', name: '任务节点', config: { canConnectFrom: ['start', 'task', 'decision'], canConnectTo: ['task', 'decision', 'end'] } },
       { type: 'decision', name: '决策节点', config: { canConnectFrom: ['start', 'task', 'decision'], canConnectTo: ['task', 'decision', 'end'] } }
     ])
     const nodes = ref([])
     const edges = ref([])
     const connectingStartNode = ref(null)
     const edgeDialogVisible = ref(false)
    const contextMenuVisible = ref(false)
    const contextMenuTarget = ref(null)
    const contextMenuPosition = ref({ x: 0, y: 0 })
     const selectedEdge = ref(null)
     const edgeConditionForm = ref({ condition: '' })
     
     // 验证连线条件表达式
     const validateCondition = (rule, value, callback) => {
       if (!value) {
         callback(new Error('请输入连线条件'))
       } else if (!/^\$\{[a-zA-Z_][a-zA-Z0-9_]*\}\s*(==|!=|>|<|>=|<=)\s*[^\s]+$/.test(value)) {
         callback(new Error('条件格式不正确，示例: ${amount} > 1000'))
       } else {
         callback()
       }
     }
     
     // 验证节点配置JSON格式
     const validateNodeConfig = (rule, value, callback) => {
       if (!value) {
         callback()
       } else {
         try {
           const config = JSON.parse(value)
           
           // 根据节点类型进行特定验证
           if (nodeForm.value.type === 'task') {
             if (!config.taskType) {
               return callback(new Error('请选择任务类型'))
             }
             if (config.taskType === 'http') {
               if (!config.url) {
                 return callback(new Error('请输入请求URL'))
               }
               if (!/^https?:\/\/[\w\-]+(\.[\w\-]+)+[\w\-.,@?^=%&:\/~+#]*$/.test(config.url)) {
                 return callback(new Error('请输入有效的HTTP URL，格式如: http://example.com'))
               }
             } else if (config.taskType === 'db') {
               if (!config.query) {
                 return callback(new Error('请输入数据库查询语句'))
               }
             } else if (config.taskType === 'script') {
               if (!config.scriptContent) {
                 return callback(new Error('请输入脚本内容'))
               }
             }
           } else if (nodeForm.value.type === 'gateway') {
             if (!config.gatewayType) {
               return callback(new Error('请选择网关类型'))
             }
           } else if (nodeForm.value.type === 'start') {
             if (!config.initParams) {
               return callback(new Error('请输入初始参数'))
             }
             try {
               JSON.parse(config.initParams)
             } catch (e) {
               return callback(new Error('初始参数必须是有效的JSON格式'))
             }
           } else if (nodeForm.value.type === 'end') {
             if (!config.resultHandler) {
               return callback(new Error('请选择结果处理方式'))
             }
           }
           
           callback()
         } catch (e) {
           callback(new Error('配置必须是有效的JSON格式'))
         }
       }
     }
     
     const handleDragStart = (e, node) => {
       e.dataTransfer.setData('nodeType', node.type)
     }
     
     const handleDragOver = (e) => {
       e.preventDefault()
     }
     
     const handleDrop = (e) => {
       e.preventDefault()
       const nodeType = e.dataTransfer.getData('nodeType')
       if (nodeType) {
         const newNode = {
           id: Date.now().toString(),
           type: nodeType,
           name: `${nodeType}节点`,
           x: e.offsetX,
           y: e.offsetY
         }
         nodes.value.push(newNode)
       }
     }
     
     // 处理连线开始
     const startConnecting = (nodeId) => {
       connectingStartNode.value = nodeId
     }
     
     // 处理连线结束
     const endConnecting = (nodeId) => {
       if (connectingStartNode.value && connectingStartNode.value !== nodeId) {
         const sourceNode = nodes.value.find(n => n.id === connectingStartNode.value)
         const targetNode = nodes.value.find(n => n.id === nodeId)
         const sourceType = nodeTypes.value.find(t => t.type === sourceNode.type)
         const targetType = nodeTypes.value.find(t => t.type === targetNode.type)
         
         if (sourceType.config.canConnectTo.includes(targetNode.type) && 
             targetType.config.canConnectFrom.includes(sourceNode.type)) {
           const newEdge = {
             id: `${connectingStartNode.value}-${nodeId}`,
             source: connectingStartNode.value,
             target: nodeId,
             condition: ''
           }
           edges.value.push(newEdge)
           selectedEdge.value = newEdge
           edgeDialogVisible.value = true
         } else {
           let errorMsg = '节点类型不匹配，无法连接'
           
           if (!sourceType.config.canConnectTo.includes(targetNode.type)) {
             errorMsg = `${sourceNode.type}节点不能连接到${targetNode.type}节点`
           } else if (!targetType.config.canConnectFrom.includes(sourceNode.type)) {
             errorMsg = `${targetNode.type}节点不能接受${sourceNode.type}节点的连接`
           }
           
           ElMessage.error(errorMsg)
         }
       }
       connectingStartNode.value = null
     }
     
     // 获取节点X坐标
     const getNodeX = (nodeId) => {
       const node = nodes.value.find(n => n.id === nodeId)
       return node ? node.x + 50 : 0 // 50是节点宽度的一半
     }
     
     // 获取节点Y坐标
     const getNodeY = (nodeId) => {
       const node = nodes.value.find(n => n.id === nodeId)
       return node ? node.y + 30 : 0 // 30是节点高度的一半
     }
     
     const saveProcessDesign = async () => {
       try {
         const response = await axios.post('/process/design', {
           nodes: nodes.value,
           edges: edges.value,
           processId: activeProcessId.value
         })
         if (response.data.success) {
           ElMessage.success('流程设计保存成功')
         }
       } catch (error) {
         console.error('保存流程设计失败:', error)
       }
     }

    const fetchProcessDefinitions = async () => {
      try {
        const response = await axios.get('/process/definitions')
        processDefinitions.value = response.data
      } catch (error) {
        console.error('获取流程定义失败:', error)
      }
    }

    const handleCreate = () => {
      dialogFormVisible.value = true
      formType.value = 'create'
      form.value = { name: '', description: '' }
    }

    const handleEdit = (process) => {
      dialogFormVisible.value = true
      formType.value = 'edit'
      form.value = { ...process }
    }

    const handleSubmit = async () => {
      try {
        if (formType.value === 'create') {
          await axios.post('/process/definitions', form.value)
        } else {
          await axios.put(`/process/definitions/${form.value.id}`, form.value)
        }
        dialogFormVisible.value = false
        await fetchProcessDefinitions()
      } catch (error) {
        console.error('保存流程失败:', error)
      }
    }

    const handleDelete = async (process) => {
      try {
        await axios.delete(`/process/definitions/${process.id}`)
        await fetchProcessDefinitions()
      } catch (error) {
        console.error('删除流程失败:', error)
      }
    }

    const fetchProcessNodes = async () => {
      try {
        const response = await axios.get('/process/nodes')
        processNodes.value = response.data
      } catch (error) {
        console.error('获取节点列表失败:', error)
      }
    }

    const handleCreateNode = () => {
      nodeDialogVisible.value = true
      nodeForm.value = { name: '', type: '', config: '' }
    }

    const handleEditNode = (node) => {
      nodeDialogVisible.value = true
      nodeForm.value = { ...node }
    }

    const handleSubmitNode = async () => {
      try {
        if (nodeForm.value.id) {
          await axios.put(`/process/nodes/${nodeForm.value.id}`, nodeForm.value)
        } else {
          await axios.post('/process/nodes', nodeForm.value)
        }
        nodeDialogVisible.value = false
        await fetchProcessNodes()
      } catch (error) {
        console.error('保存节点失败:', error)
      }
    }

    const handleDeleteNode = async (node) => {
      try {
        await axios.delete(`/process/nodes/${node.id}`)
        await fetchProcessNodes()
      } catch (error) {
        console.error('删除节点失败:', error)
      }
    }

    onMounted(() => {
      fetchProcessDefinitions()
    })

    const handleEdgeContextMenu = (edge, e) => {
      e.preventDefault()
      contextMenuTarget.value = edge
      contextMenuPosition.value = { x: e.clientX, y: e.clientY }
      contextMenuVisible.value = true
    }
    
    // 右键菜单组件
    const ContextMenu = {
      template: `
        <div 
          v-show="visible" 
          class="context-menu" 
          :style="{ left: position.x + 'px', top: position.y + 'px' }"
        >
          <div class="menu-item" @click="openEdgeDialog">编辑条件</div>
          <div class="menu-item" @click="deleteSelectedEdge">删除连线</div>
        </div>
      `,
      props: ['visible', 'position'],
      methods: {
        openEdgeDialog() {
          this.$emit('open-edge-dialog')
        },
        deleteSelectedEdge() {
          this.$emit('delete-selected-edge')
        }
      }
    }
    
    const openEdgeDialog = () => {
      if (contextMenuTarget.value) {
        selectedEdge.value = contextMenuTarget.value
        edgeConditionForm.value.condition = selectedEdge.value.condition || ''
        edgeDialogVisible.value = true
        contextMenuVisible.value = false
      }
    }
    
    const deleteSelectedEdge = () => {
      if (contextMenuTarget.value) {
        edges.value = edges.value.filter(e => e.id !== contextMenuTarget.value.id)
        contextMenuVisible.value = false
      }
    }
    
    const handleSubmitEdgeCondition = () => {
      if (selectedEdge.value) {
        selectedEdge.value.condition = edgeConditionForm.value.condition
        edgeDialogVisible.value = false
      }
    }
    
    const handleDeleteEdge = () => {
      edges.value = edges.value.filter(e => e.id !== selectedEdge.value.id)
      edgeDialogVisible.value = false
    }
    
    return {
      processDefinitions,
      processNodes,
      handleCreate,
      handleEdit,
      handleDelete,
      handleCreateNode,
      handleEditNode,
      handleDeleteNode,
      handleSubmitNode,
      dialogFormVisible,
      nodeDialogVisible,
      formType,
      activeTab,
      form,
      nodeForm,
      handleSubmit,
      nodeTypes,
      nodes,
      edges,
      handleDragStart,
      handleDragOver,
      handleDrop,
      edgeDialogVisible,
      edgeConditionForm,
      handleEdgeContextMenu,
      handleSubmitEdgeCondition,
      handleDeleteEdge,
      ContextMenu,
      contextMenuVisible,
      contextMenuTarget,
      contextMenuPosition
    }
  }
}
</script>

<style scoped>
.process {
  padding: 20px;
}

.designer-container {
  display: flex;
  height: 600px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.node-palette {
  width: 200px;
  padding: 10px;
  border-right: 1px solid #dcdfe6;
  background-color: #f5f7fa;
}

.node-item {
  padding: 8px 12px;
  margin-bottom: 8px;
  background-color: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: move;
}

.node-item:hover {
  background-color: #f5f7fa;
}

.designer-area {
  flex: 1;
  position: relative;
  background-color: #fff;
  overflow: hidden;
}

.node {
  position: absolute;
  width: 100px;
  height: 60px;
  background-color: #fff;
  border: 1px solid #409EFF;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  user-select: none;
}

.edges {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.context-menu {
  position: fixed;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 9999;
}

.menu-item {
  padding: 8px 16px;
  cursor: pointer;
}

.menu-item:hover {
  background-color: #f5f7fa;
}
</style>