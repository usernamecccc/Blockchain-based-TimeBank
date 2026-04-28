<template>
  <div class="AdminContainer">
    <!-- 查询 -->
    <div class="search">
      <div class="searchContainer">
        <el-input type="text" v-model="searchTitle" prefix-icon="el-icon-search" placeholder="活动标题" style="vertical-align: middle;margin-right: 20px;width: auto;"></el-input>
        <el-input type="text" v-model="searchAddress" prefix-icon="el-icon-search" placeholder="活动地点" style="vertical-align: middle;margin-right: 20px;width: auto;"></el-input>
        <div class="block">
          <el-date-picker
            v-model="searchDate"
            align="right"
            type="date"
            placeholder="活动日期"
            :picker-options="pickerOptions"
            style="vertical-align: middle;margin-right: 20px;width: auto;">
          </el-date-picker>
        </div>
        <el-time-picker
          v-model="searchBegin"
          placeholder="活动开始时间"
          style="vertical-align: middle;margin-right: 20px;width: auto;">
        </el-time-picker>
        <el-time-picker
          v-model="searchEnd"
          placeholder="活动结束时间"
          style="vertical-align: middle;margin-right: 20px;">
        </el-time-picker>
        <el-select v-model="searchStatus" clearable placeholder="活动状态" style="width: auto;" @change="syncTreeHighlightFromStatus">
          <el-option
            v-for="item in options"
            :key="item.value"
            :label="item.label"
            :value="item.value">
          </el-option>
        </el-select>
        <span class="tree-hint">左侧可按状态快速筛选</span>
      </div>
      <div class="searchContainer">
        <el-button type="primary" round icon="el-icon-search" @click="search" style="height: 35px; margin-left: auto;">搜索</el-button>
        <el-button type="primary" round icon="el-icon-refresh" @click="clearSearch" style="height: 35px;">重置</el-button>
        <el-button type="primary" round icon="el-icon-circle-plus" @click="addTable" style="height: 35px;">添加</el-button>
        <el-button type="primary" round icon="el-icon-delete" @click="deleteTable" style="height: 35px; margin-right: 20px;">删除</el-button>
      </div>
    </div>
    <div class="contentBox">
      <!-- 树控件 -->
      <div class="left">
        <el-input
          placeholder="输入关键字进行过滤"
          v-model="filterText">
        </el-input>

        <el-tree
          class="filter-tree"
          :data="treeData"
          :props="defaultProps"
          node-key="id"
          highlight-current
          default-expand-all
          :filter-node-method="filterNode"
          @node-click="handleTreeCategoryClick"
          ref="tree">
        </el-tree>
      </div>
      <div class="right">
        <!-- 表格 -->
        <el-table
          :data="tableData"
          border
          style="border: 1px solid #ccc;"
          show-selection @selection-change="handleSelectionChange">
          <!-- 表格列定义 -->
          <el-table-column type="selection" width="45"></el-table-column>
          <el-table-column fixed prop="id" label="id" width="80"></el-table-column>
          <el-table-column prop="title" label="活动标题" width="100"></el-table-column>
          <el-table-column prop="quota" label="活动名额" width="110" sortable></el-table-column>
          <el-table-column prop="date" label="活动时期" width="110" sortable></el-table-column>
          <el-table-column prop="begin" label="活动开始时间" width="150" sortable></el-table-column>
          <el-table-column prop="end" label="活动结束时间" width="150" sortable></el-table-column>
          <el-table-column prop="address" label="地址" width="300"></el-table-column>
          <el-table-column prop="oldId" label="老人ID" width="80"></el-table-column>
          <el-table-column prop="phone" label="发布人电话" width="100"></el-table-column>
          <el-table-column prop="status" label="活动状态" width="100">
            <template slot-scope="scope">
              <el-tag :type="getTagType(scope.row.status)">
                {{ getTagText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="活动活动描述" width="150">
            <template slot-scope="scope">
              <!-- 点击按钮触发打开弹窗事件 -->
              <el-button type="text" @click="openDialog(scope.row.description)">点击查看描述</el-button>
            </template>
          </el-table-column>
          <!-- 操作列 -->
          <el-table-column
            fixed="right"
            label="操作"
            width="180">
            <template slot-scope="scope">
              <div style="display: flex; justify-content: center;">
                <el-button size = "small" @click="queryForm(scope.row)" style="margin-right: 10px;">查看</el-button>
                <el-button size = "small" @click="editForm(scope.row)">编辑</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
    <!-- 分页组件 -->
    <div class="pagination-container">
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="currentPage"
        :page-sizes="[5, 10, 20, 30]" 
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="totalItems">
      </el-pagination>
    </div>
    <!-- 抽屉 -->
    <el-drawer
      title="活动信息"
      :visible.sync="drawer"
      :before-close="handleClose"
      direction="ltr"
      :append-to-body="true"
      :modal-append-to-body="false">
      <el-form ref="form" :model="form" :disabled="formDisabled" label-width="100px">
        <el-form-item label="活动ID">
          <el-input v-model="form.id" :disabled='isFormIdDisabled'></el-input>
        </el-form-item>
        <el-form-item label="活动标题">
          <el-input v-model="form.title"></el-input>
        </el-form-item>
        <el-form-item label="活动名额">
          <el-input-number v-model="form.quota" :min="1"></el-input-number>
        </el-form-item>
        <el-form-item label="报名截止时间">
          <div class="block">
            <el-date-picker
              v-model="form.deadline"
              type="datetime"
              placeholder="选择日期时间"
              align="right"
              :picker-options="pickerOptions">
            </el-date-picker>
          </div>
        </el-form-item>
        <el-form-item label="活动日期">
          <el-date-picker type="date" v-model="form.date" style="width: 100%;"></el-date-picker>
        </el-form-item>
        <el-form-item label="活动开始时间">
          <el-input v-model="form.begin"></el-input>
        </el-form-item>
        <el-form-item label="活动结束时间">
          <el-input v-model="form.end"></el-input>
        </el-form-item>
        <el-form-item label="活动地址">
          <el-input v-model="form.address"></el-input>
        </el-form-item>
        <el-form-item label="老人ID">
          <el-input v-model="form.oldId"></el-input>
        </el-form-item>
        <el-form-item label="发布人电话">
          <el-input v-model="form.phone"></el-input>
        </el-form-item>
        <el-form-item label="活动描述">
          <el-input type="textarea" v-model="form.description"></el-input>
        </el-form-item>
        <el-form-item label="活动状态">
          <el-select v-model="form.status" clearable placeholder="请选择" style="margin-right: 20px;">
            <el-option
              v-for="item in options"
              :key="item.value"
              :label="item.label"
              :value="item.value">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="管理员ID">
          <el-input v-model="form.administratorId" :disabled=true></el-input>
        </el-form-item>
        <el-form-item label="活动创建时间">
          <div class="block">
            <el-date-picker
              v-model="form.createTime"
              type="datetime"
              placeholder="选择日期时间"
              align="right"
              :picker-options="pickerOptions"
              :disabled=true>
            </el-date-picker>
          </div>
        </el-form-item>
        <el-form-item label="活动更新时间">
          <div class="block">
            <el-date-picker
              v-model="form.updateTime"
              type="datetime"
              placeholder="选择日期时间"
              align="right"
              :picker-options="pickerOptions"
              :disabled=true>
            </el-date-picker>
          </div>
        </el-form-item>
        <el-form-item label="活动剩余名额">
          <el-input-number v-model="form.remain" :min="0" :disabled='isFormRemainDisabled'></el-input-number>
        </el-form-item>
        <el-form-item label="管理员建议">
          <el-input type="textarea" v-model="form.message"></el-input>
        </el-form-item>
        <el-form-item style="display: flex; justify-content: flex-end; align-items: center;">
          <el-button type="primary" @click="onSubmitForm">提 交</el-button>
        </el-form-item>
      </el-form>
    </el-drawer>
  </div>
</template>

<script>
import { MessageBox } from 'element-ui';// 需要单独导入
import request from '@/utils/request';

export default {
  name: 'AdminView',
  data() {
    return {
      // 日期表
      pickerOptions: {
        shortcuts: [{
          text: '今天',
          onClick(picker) {
            picker.$emit('pick', new Date());
          }
        }, {
          text: '昨天',
          onClick(picker) {
            const date = new Date();
            date.setTime(date.getTime() - 3600 * 1000 * 24);
            picker.$emit('pick', date);
          }
        }, {
          text: '一周前',
          onClick(picker) {
            const date = new Date();
            date.setTime(date.getTime() - 3600 * 1000 * 24 * 7);
            picker.$emit('pick', date);
          }
        }]
      },
      // 与后端一致：1待审 2审核通过 3进行中 4拒绝 5活动过期
      options: [
        { value: 1, label: '待审核' },
        { value: 2, label: '审核通过' },
        { value: 3, label: '进行中' },
        { value: 4, label: '拒绝进行' },
        { value: 5, label: '活动过期' },
      ],
      // 搜索数据
      searchTitle: '', // 活动标题
      searchAddress: '', // 活动地点
      searchDate: '', // 活动日期
      searchBegin: '',
      searchEnd: '',
      searchStatus: '', // 活动状态
      // 表格数据
      originalData: {},
      pageSize: 5, // 每页显示的条目数量
      totalItems: 0, // 总条目数量
      currentPage: 1, // 当前页码
      tableData: [], // 表格数据
      selectedIds: [],
      // 表单
      drawer: false,
      form: {},
      formDisabled: true, // 禁用整个表单
      isFormIdDisabled: false,
      isFormRemainDisabled: false,
      // 树控组件
      filterText: '',
      // 左侧分类：与业务库中 activity.status 对应（原「专业类」等为占位，无字段支撑）
      treeData: [
        { id: 'all', label: '全部活动' },
        { id: 's1', label: '待审核', status: 1 },
        { id: 's2', label: '审核通过', status: 2 },
        { id: 's3', label: '进行中', status: 3 },
        { id: 's4', label: '拒绝进行', status: 4 },
        { id: 's5', label: '活动过期', status: 5 },
      ],
      defaultProps: {
        children: 'children',
        label: 'label'
      },
    };
  },
  mounted() {
    this.search();
    this.isFormItemDisabled();
    this.$nextTick(() => {
      if (this.$refs.tree) this.$refs.tree.setCurrentKey('all');
    });
  },
  watch: {
    filterText(val) {
      this.$refs.tree.filter(val);
    }
  },
  methods: {
    // 过滤树节点
    filterNode(value, data) {
      if (!value) return true;
      return data.label.indexOf(value) !== -1;
    },
    /** 点击左侧分类：按活动状态筛选并请求列表 */
    syncTreeHighlightFromStatus(val) {
      const map = { 1: 's1', 2: 's2', 3: 's3', 4: 's4', 5: 's5' };
      this.$nextTick(() => {
        if (!this.$refs.tree) return;
        const key = val === null || val === '' || val === undefined ? 'all' : (map[val] || 'all');
        this.$refs.tree.setCurrentKey(key);
      });
    },
    handleTreeCategoryClick(data) {
      if (data.id === 'all') {
        this.searchStatus = '';
      } else if (typeof data.status === 'number') {
        this.searchStatus = data.status;
      } else {
        return;
      }
      this.currentPage = 1;
      this.search();
      this.$nextTick(() => {
        if (this.$refs.tree) this.$refs.tree.setCurrentKey(data.id);
      });
    },
    formatSearchTimeToHMS(val) {
      if (val == null || val === '') return '';
      if (val instanceof Date && !isNaN(val.getTime())) {
        const h = String(val.getHours()).padStart(2, '0');
        const m = String(val.getMinutes()).padStart(2, '0');
        const s = String(val.getSeconds()).padStart(2, '0');
        return `${h}:${m}:${s}`;
      }
      const d = new Date(val);
      if (!isNaN(d.getTime())) {
        const h = String(d.getHours()).padStart(2, '0');
        const m = String(d.getMinutes()).padStart(2, '0');
        const s = String(d.getSeconds()).padStart(2, '0');
        return `${h}:${m}:${s}`;
      }
      return '';
    },
    openDialog(description) {
      // 打开弹窗显示活动描述
      MessageBox.alert(description, '活动描述', {
        confirmButtonText: '确定',
        type: 'info'
      });
    },
    handleCurrentChange(newPage) {
      // 更新当前页码
      this.currentPage = newPage;
      // 重新搜索获取对应页的数据
      this.search();
    },
    handleSizeChange(newSize) {
      // 更新页面大小
      this.pageSize = newSize;
      // 重新搜索获取对应页的数据
      this.search();
    },
    search() {
      // 格式化日期
      const formatDateString = (dateString) => {
        if (!dateString) return '';
        const date = new Date(dateString);
        const year = date.getFullYear();
        const month = date.getMonth() + 1 < 10 ? '0' + (date.getMonth() + 1) : date.getMonth() + 1;
        const day = date.getDate() < 10 ? '0' + date.getDate() : date.getDate();
        return `${year}-${month}-${day}`;
      };
      // 创建 URLSearchParams 对象
      const params = new URLSearchParams();
      // 添加搜索条件到 URLSearchParams 对象中
      params.append('title', this.searchTitle);
      params.append('address', this.searchAddress);
      params.append('date', formatDateString(this.searchDate));
      params.append('begin', this.formatSearchTimeToHMS(this.searchBegin));
      params.append('end', this.formatSearchTimeToHMS(this.searchEnd));
      params.append('status', this.searchStatus);
      params.append('pageSize', this.pageSize);
      params.append('page', this.currentPage);
      // 将 URLSearchParams 对象转换为查询字符串
      const queryString = params.toString();

      // 发起请求时将查询字符串添加到URL中
      request.get(`/administrator?${queryString}`)
        .then(response => {
          if (response.code === 1) {
            this.totalItems = response.data.total;
            this.originalData = response.data.rows;
            this.tableData = [];
            // 合并原始数据到 tableData 数组中
            this.tableData = [...this.tableData, ...this.originalData];
          } else {
            this.$message.error(response.msg);
          }
        })
        .catch(error => {
          console.error('获取数据失败:', error);
        });
    },
    clearSearch() {
      this.searchTitle = '';
      this.searchAddress = '';
      this.searchDate = '';
      this.searchBegin = '';
      this.searchEnd = '';
      this.searchStatus = '';
      this.currentPage = 1;
      this.search();
      this.$nextTick(() => {
        if (this.$refs.tree) this.$refs.tree.setCurrentKey('all');
      });
    },
    // 不同标签
    getTagType(status) {
      switch (status) {
        case 1:
          return 'warning';
        case 2:
          return 'success';
        case 3:
          return 'primary';
        case 4:
          return 'danger';
        case 5:
          return 'info';
        default:
          return '';
      }
    },
    getTagText(status) {
      switch (status) {
        case 1:
          return '待审核';
        case 2:
          return '审核通过';
        case 3:
          return '进行中';
        case 4:
          return '拒绝进行';
        case 5:
          return '活动过期';
        default:
          return '';
      }
    },
    // 表单
    queryForm(rowData) {
      this.form = { ...rowData};
      // 打开抽屉
      this.drawer = true;
    },
    editForm(rowData){
      this.form = { ...rowData};
      this.formDisabled = false;
      // 打开抽屉
      this.drawer = true;
    },
    handleClose(done) {
      this.$confirm('确认关闭？')
        .then(() => {
          this.formDisabled = true;
          done();
        })
        .catch(() => {});
    },
    isFormItemDisabled(){
      // 检查表单项是否禁用
      return this.formDisabled;
    },
    onSubmitForm(){
      // 正则表达式,用于匹配时分秒形式(HH:mm:ss)
      const timePattern = /^(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d$/;
      if(!timePattern.test(this.form.begin)){
        this.$message.warning("输入的活动开始时间格式错误");
        return;
      }
      if(!timePattern.test(this.form.end)){
        this.$message.warning("输入的活动结束时间格式错误");
        return;
      }
      // 格式化日期
      const formatDateString = (dateString) => {
        if (!dateString) return '';
        const date = new Date(dateString);
        const year = date.getFullYear();
        const month = date.getMonth() + 1 < 10 ? '0' + (date.getMonth() + 1) : date.getMonth() + 1;
        const day = date.getDate() < 10 ? '0' + date.getDate() : date.getDate();
        return `${year}-${month}-${day}`;
      };
      // 在这个方法中获取搜索框中的数据
      const data = {};
      for (const key in this.form) {
        if (!this.isFormItemDisabled(key)) {
          data[key] = this.form[key];
        }
      }
      // 格式化截止时间
      //data.deadline = format(data.deadline, "yyyy-MM-dd'T'HH:mm:ss", { timeZone: 'Asia/BeiJing' });
      // 格式化日期
      data.date = formatDateString(data.date);
      request.put('/administrator',data)
        .then(response => {
          if(response.code === 1){
            this.$message.success(response.msg);
            this.search();
            this.formDisabled = true;
            this.drawer = false;
          }
          else{
            this.$message.error(response.msg);
          }
        })
        .catch(error => {
          console.error('There was a problem with the request:', error);
        });
    },
    addTable(){
      // 使用 $router.push() 方法进行路由跳转
      this.$router.push('/addActivityView');
    },
    // 表格选择
    handleSelectionChange(selection) {
      this.selectedIds = selection.map(item => item.id);
    },
    // 删除
    deleteTable(){
      const ids = this.selectedIds.join(',');
      if(ids === ''){
        this.$message.error("请选择要删除的用户");
        return;
      }
      // 发起删除请求
      request.delete(`/administrator/${ids}`)
        .then(response => {
          if(response.code === 1){
            this.$message.success(response.msg);
            this.search();
          }
          else{
            this.$message.error(response.msg);
          }
        })
        .catch(error => {
          console.error('There was a problem with the request:', error);
        });
    }
  }
};
</script>

<style scoped>
.AdminContainer{
  display: flex;
  flex-direction: column;
}
.search{
  height: 110px;
  border: 1px solid #ccc;
  margin-bottom: 10px;
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  margin-bottom: 20px;
  background-color: #fff;
}
.searchContainer{
  height: 50px;
  margin: 10px;
  display: flex;
  align-items: center;
  justify-content: center; 
}
.pagination-container {
  display: flex;
  justify-content: center;
  margin: 10px;
}
.el-form-item {
  margin-right: 20px;
  margin-left: 20px;
}
.contentBox{
  display: flex;
  flex-direction: row;
}
.tree-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}
.left{
  width: 18%;
  border-right: 1px solid #ccc;
  padding: 10px;
  border-radius: 20px;
  background-color: #fff;
  margin-right: 20px;
}
.left ::v-deep .el-tree-node.is-current > .el-tree-node__content {
  background-color: #ecf5ff;
  color: #409eff;
}
.right{
  width: 80%;
  padding: 10px;
  border-radius: 20px;
  background-color: #fff;
}
</style>
