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
            v-for="item in statusFilterOptions"
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
      <div class="ai-review-panel">
        <div class="ai-review-header">
          <div>
            <span>AI审核辅助</span>
            <el-tag v-if="aiReview" size="mini" :type="reviewTagType(aiReview.recommendation)">
              {{ reviewRecommendationText(aiReview.recommendation) }}
            </el-tag>
          </div>
          <div class="ai-review-actions">
            <el-button v-if="aiReview" size="mini" type="text" @click="aiReviewExpanded = !aiReviewExpanded">
              {{ aiReviewExpanded ? '收起' : '详情' }}
            </el-button>
            <el-button size="mini" type="primary" :loading="aiReviewLoading" @click="generateActivityReview">生成建议</el-button>
          </div>
        </div>
        <div v-if="aiReview" class="ai-review-summary">{{ aiReview.summary }}</div>
        <div v-if="aiReview && aiReviewExpanded" class="ai-review-body">
          <div v-if="aiReview.issues && aiReview.issues.length" class="ai-review-list">
            <div v-for="item in aiReview.issues" :key="item">· {{ item }}</div>
          </div>
          <div v-if="aiReview.suggestedMessage" class="ai-review-message">
            建议说明：{{ aiReview.suggestedMessage }}
          </div>
          <el-button v-if="!formDisabled" size="mini" plain @click="applyReviewMessage">写入管理员建议</el-button>
        </div>
      </div>
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
        <el-form-item label="每人答谢">
          <el-input-number v-model="form.volunteerReward" :min="0" :precision="0" placeholder="链上答谢，0 表示不答谢"></el-input-number>
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
          <el-input v-model="form.oldId" placeholder="一般为 old 表主键；也可填对应用户的 user.id"></el-input>
        </el-form-item>
        <el-form-item label="发布人电话">
          <el-input v-model="form.phone"></el-input>
        </el-form-item>
        <el-form-item label="活动描述">
          <el-input type="textarea" v-model="form.description"></el-input>
        </el-form-item>
        <el-form-item label="服务类型">
          <el-select v-model="form.serviceType" placeholder="请选择服务类型" style="width: 100%;" @change="onAdminServiceTypeChange">
            <el-option
              v-for="(label, key) in serviceTypeOptions"
              :key="key"
              :label="label"
              :value="key">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item v-for="field in dynamicExtraFields" :key="field.key" :label="field.label">
          <el-input
            v-if="field.type === 'textarea'"
            type="textarea"
            v-model.trim="form.extra[field.key]"
            :placeholder="field.placeholder">
          </el-input>
          <el-select
            v-else-if="field.type === 'boolean'"
            v-model="form.extra[field.key]"
            placeholder="请选择"
            style="width: 100%;">
            <el-option label="是" :value="true"></el-option>
            <el-option label="否" :value="false"></el-option>
          </el-select>
          <el-input
            v-else
            v-model.trim="form.extra[field.key]"
            :placeholder="field.placeholder">
          </el-input>
        </el-form-item>
        <el-form-item label="活动状态">
          <el-select v-model="form.status" clearable placeholder="请选择审核状态" style="margin-right: 20px;">
            <el-option
              v-for="item in editStatusSelectOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
              :disabled="!!item.disabled">
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
        <el-form-item v-if="!formDisabled" style="display: flex; justify-content: flex-end; align-items: center;">
          <el-button type="primary" @click="onSubmitForm">提 交</el-button>
        </el-form-item>
      </el-form>
    </el-drawer>
  </div>
</template>

<script>
import { MessageBox } from 'element-ui';// 需要单独导入
import request from '@/utils/request';
import { format } from 'date-fns-tz';

/** 列表筛选 / 左侧树：全部状态（与后端 activity.status 一致） */
const ALL_ACTIVITY_STATUS_OPTIONS = Object.freeze([
  { value: 1, label: '待审核' },
  { value: 2, label: '审核通过' },
  { value: 3, label: '进行中' },
  { value: 4, label: '拒绝进行' },
  { value: 5, label: '活动过期' },
]);

/** 编辑抽屉：仅允许改为这三种审核结论（1 / 2 / 4） */
const AUDIT_STATUS_OPTIONS = Object.freeze([
  { value: 1, label: '待审核' },
  { value: 2, label: '审核通过' },
  { value: 4, label: '拒绝进行' },
]);

const SERVICE_TYPE_LABELS = Object.freeze({
  medical_rehab: '医疗康复',
  health_manage: '健康管理',
  cleaning: '清洁整理',
  shopping_companion: '购物陪同',
  clinic_companion: '问诊陪护',
  purchase: '物品代购',
  other_service: '其他服务',
});

const SERVICE_TYPE_FIELD_CONFIG = Object.freeze({
  medical_rehab: [
    { key: 'hospitalAddress', label: '医院地址', placeholder: '请输入医院详细地址', required: true },
    { key: 'department', label: '科室', placeholder: '如：内科/康复科', required: true },
    { key: 'appointmentTime', label: '预约时间', placeholder: '如：2026-05-20 09:00', required: true },
  ],
  health_manage: [
    { key: 'healthTaskType', label: '健康任务类型', placeholder: '如：服药提醒/血压测量', required: true },
    { key: 'frequency', label: '服务频次', placeholder: '如：每周 3 次', required: true },
  ],
  cleaning: [
    { key: 'cleaningScope', label: '清洁范围', placeholder: '如：厨房+卫生间', required: true },
    { key: 'homeArea', label: '房屋面积', placeholder: '如：80 平米', required: true },
  ],
  shopping_companion: [
    { key: 'destination', label: '目的地', placeholder: '如：XX 商超', required: true },
    { key: 'budgetRange', label: '预算范围', placeholder: '如：100-300 元', required: true },
  ],
  clinic_companion: [
    { key: 'hospitalAddress', label: '医院地址', placeholder: '请输入医院详细地址', required: true },
    { key: 'visitType', label: '就诊类型', placeholder: '如：复诊/检查', required: true },
    { key: 'registrationNeeded', label: '是否需要协助挂号', type: 'boolean', required: true },
  ],
  purchase: [
    { key: 'shoppingList', label: '代购清单', type: 'textarea', placeholder: '请填写物品名、规格、数量', required: true },
    { key: 'maxBudget', label: '最高预算', placeholder: '如：200 元', required: true },
  ],
  other_service: [
    { key: 'customCategory', label: '自定义服务类别', placeholder: '如：上门陪聊', required: true },
    { key: 'serviceDetails', label: '服务详情', type: 'textarea', placeholder: '请详细说明需求', required: true },
  ],
});

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
      statusFilterOptions: ALL_ACTIVITY_STATUS_OPTIONS.map((o) => ({ ...o })),
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
      form: {
        serviceType: 'other_service',
        extra: {},
        volunteerReward: 0,
      },
      formDisabled: true, // 禁用整个表单
      isFormIdDisabled: false,
      isFormRemainDisabled: false,
      aiReview: null,
      aiReviewLoading: false,
      aiReviewExpanded: false,
      // 树控组件
      filterText: '',
      // 左侧分类：与业务库 activity.status 对应
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
    this.$nextTick(() => {
      if (this.$refs.tree) this.$refs.tree.setCurrentKey('all');
    });
  },
  watch: {
    filterText(val) {
      this.$refs.tree.filter(val);
    }
  },
  computed: {
    serviceTypeOptions() {
      return SERVICE_TYPE_LABELS;
    },
    /** 抽屉内状态下拉：默认可改三种审核状态；若当前为进行中/过期则插入一条禁用项便于辨认 */
    editStatusSelectOptions() {
      const rows = AUDIT_STATUS_OPTIONS.map((o) => ({ ...o }));
      const s = this.form && this.form.status;
      const n = s !== '' && s !== null && s !== undefined ? Number(s) : NaN;
      if (n === 3 || n === 5) {
        rows.unshift({
          value: n,
          label: n === 3 ? '进行中（当前非审核态，请改为上述三种之一再保存）' : '活动过期（当前非审核态，请改为上述三种之一再保存）',
          disabled: true,
        });
      }
      return rows;
    },
    dynamicExtraFields() {
      const serviceType = this.normalizeServiceType(this.form.serviceType);
      return SERVICE_TYPE_FIELD_CONFIG[serviceType] || SERVICE_TYPE_FIELD_CONFIG.other_service;
    },
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
    /** 与新增页一致：将开始/结束时间规范为 HH:mm:ss，避免后端 LocalTime 反序列化失败 */
    formatWallTimeToHMS(val) {
      if (val == null || val === '') return '';
      if (val instanceof Date && !Number.isNaN(val.getTime())) {
        const h = String(val.getHours()).padStart(2, '0');
        const m = String(val.getMinutes()).padStart(2, '0');
        const s = String(val.getSeconds()).padStart(2, '0');
        return `${h}:${m}:${s}`;
      }
      const d = new Date(val);
      if (!Number.isNaN(d.getTime())) {
        const h = String(d.getHours()).padStart(2, '0');
        const m = String(d.getMinutes()).padStart(2, '0');
        const s = String(d.getSeconds()).padStart(2, '0');
        return `${h}:${m}:${s}`;
      }
      if (typeof val === 'string') {
        const m = val.trim().match(/^(\d{1,2}):(\d{2})(?::(\d{2}))?/);
        if (m) {
          return `${m[1].padStart(2, '0')}:${m[2]}:${(m[3] || '00').padStart(2, '0')}`;
        }
      }
      return '';
    },
    normalizeServiceType(serviceType) {
      return SERVICE_TYPE_LABELS[serviceType] ? serviceType : 'other_service';
    },
    parseExtraJson(extraJson) {
      if (!extraJson) return {};
      try {
        const parsed = typeof extraJson === 'string' ? JSON.parse(extraJson) : extraJson;
        if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
          return parsed;
        }
      } catch (error) {
        // Ignore malformed legacy extraJson
      }
      return {};
    },
    ensureDynamicExtraShape() {
      if (!this.form.extra || typeof this.form.extra !== 'object' || Array.isArray(this.form.extra)) {
        this.$set(this.form, 'extra', {});
      }
      this.dynamicExtraFields.forEach((field) => {
        if (Object.prototype.hasOwnProperty.call(this.form.extra, field.key)) return;
        this.$set(this.form.extra, field.key, field.type === 'boolean' ? null : '');
      });
    },
    hydrateDynamicServiceData(rowData) {
      const serviceType = this.normalizeServiceType(rowData.serviceType);
      const parsedExtra = this.parseExtraJson(rowData.extraJson);
      this.form.serviceType = serviceType;
      this.$set(this.form, 'extra', parsedExtra);
      this.ensureDynamicExtraShape();
    },
    onAdminServiceTypeChange() {
      this.form.serviceType = this.normalizeServiceType(this.form.serviceType);
      this.ensureDynamicExtraShape();
    },
    validateDynamicExtraFields() {
      this.ensureDynamicExtraShape();
      for (const field of this.dynamicExtraFields) {
        if (!field.required) continue;
        const val = this.form.extra[field.key];
        if (field.type === 'boolean') {
          if (val === null || val === undefined) {
            this.$message.warning(`请填写：${field.label}`);
            return false;
          }
          continue;
        }
        if (val === null || val === undefined || String(val).trim() === '') {
          this.$message.warning(`请填写：${field.label}`);
          return false;
        }
      }
      return true;
    },
    buildExtraPayload() {
      const payload = {};
      this.dynamicExtraFields.forEach((field) => {
        const val = this.form.extra[field.key];
        if (field.type === 'boolean') {
          if (val !== null && val !== undefined) payload[field.key] = val;
          return;
        }
        if (val !== null && val !== undefined && String(val).trim() !== '') {
          payload[field.key] = val;
        }
      });
      return payload;
    },
    // 表单
    queryForm(rowData) {
      this.form = { ...rowData, volunteerReward: rowData.volunteerReward ?? 0 };
      this.hydrateDynamicServiceData(rowData);
      this.formDisabled = true;
      this.aiReview = null;
      this.aiReviewExpanded = false;
      this.drawer = true;
    },
    editForm(rowData){
      this.form = { ...rowData, volunteerReward: rowData.volunteerReward ?? 0 };
      this.hydrateDynamicServiceData(rowData);
      this.formDisabled = false;
      this.aiReview = null;
      this.aiReviewExpanded = false;
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
    onSubmitForm(){
      if (this.formDisabled) {
        this.$message.warning('请先点击「编辑」再保存');
        return;
      }
      const beginHms = this.formatWallTimeToHMS(this.form.begin);
      const endHms = this.formatWallTimeToHMS(this.form.end);
      if (!beginHms || !endHms) {
        this.$message.warning('活动开始/结束时间格式有误，请使用 HH:mm 或 HH:mm:ss');
        return;
      }
      const timePattern = /^(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d$/;
      if (!timePattern.test(beginHms) || !timePattern.test(endHms)) {
        this.$message.warning('活动开始/结束时间须为合法的 24 小时制时间');
        return;
      }
      if (!this.form.deadline) {
        this.$message.warning('请填写报名截止时间');
        return;
      }
      if (!this.validateDynamicExtraFields()) {
        return;
      }
      const deadlineDt = this.form.deadline instanceof Date ? this.form.deadline : new Date(this.form.deadline);
      if (Number.isNaN(deadlineDt.getTime())) {
        this.$message.warning('报名截止时间无效');
        return;
      }

      const formatDateString = (dateString) => {
        if (!dateString) return '';
        const date = new Date(dateString);
        if (Number.isNaN(date.getTime())) return '';
        const year = date.getFullYear();
        const month = date.getMonth() + 1 < 10 ? '0' + (date.getMonth() + 1) : date.getMonth() + 1;
        const day = date.getDate() < 10 ? '0' + date.getDate() : date.getDate();
        return `${year}-${month}-${day}`;
      };

      const dateStr = formatDateString(this.form.date);
      if (!dateStr) {
        this.$message.warning('请填写活动日期');
        return;
      }

      const statusNum = this.form.status !== '' && this.form.status !== null && this.form.status !== undefined
        ? Number(this.form.status)
        : NaN;
      if (![1, 2, 4].includes(statusNum)) {
        this.$message.warning('活动状态仅能保存为：待审核、审核通过或拒绝进行');
        return;
      }

      const data = {
        id: this.form.id != null ? Number(this.form.id) : undefined,
        title: this.form.title,
        quota: this.form.quota != null ? Number(this.form.quota) : undefined,
        deadline: format(deadlineDt, "yyyy-MM-dd'T'HH:mm:ss", { timeZone: 'Asia/Shanghai' }),
        date: dateStr,
        begin: beginHms,
        end: endHms,
        address: this.form.address,
        oldId: this.form.oldId != null && this.form.oldId !== '' ? Number(this.form.oldId) : undefined,
        phone: this.form.phone,
        description: this.form.description,
        status: statusNum,
        remain: this.form.remain != null ? Number(this.form.remain) : undefined,
        message: this.form.message,
        volunteerReward: Number(this.form.volunteerReward ?? 0),
        serviceType: this.normalizeServiceType(this.form.serviceType),
        extraJson: JSON.stringify(this.buildExtraPayload()),
      };

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
    generateActivityReview() {
      if (!this.form || !this.form.id) {
        this.$message.warning('请先选择活动');
        return;
      }
      this.aiReviewLoading = true;
      request.post('/users/administrator/ai/activity-review', { activity: this.form }, { timeout: 150000 })
        .then((response) => {
          if (response.code === 1 && response.data) {
            this.aiReview = response.data;
            this.aiReviewExpanded = false;
          } else {
            this.$message.error(response.msg || 'AI审核建议生成失败');
          }
        })
        .catch(() => {
          this.$message.error('AI服务暂时不可用');
        })
        .finally(() => {
          this.aiReviewLoading = false;
        });
    },
    reviewRecommendationText(value) {
      const map = {
        approve: '可通过',
        revise: '需修改',
        reject: '建议拒绝',
      };
      return map[value] || '仅供参考';
    },
    reviewTagType(value) {
      const map = {
        approve: 'success',
        revise: 'warning',
        reject: 'danger',
      };
      return map[value] || 'info';
    },
    applyReviewMessage() {
      if (!this.aiReview || !this.aiReview.suggestedMessage) return;
      this.form.message = this.aiReview.suggestedMessage;
      if (this.aiReview.recommendation === 'approve') {
        this.form.status = 2;
      } else if (this.aiReview.recommendation === 'reject') {
        this.form.status = 4;
      }
      this.$message.success('已写入审核建议');
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
.ai-review-panel {
  margin: 0 20px 16px;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #f8fbff;
}
.ai-review-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  color: #303133;
}
.ai-review-header > div:first-child {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ai-review-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ai-review-body {
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
}
.ai-review-summary,
.ai-review-list,
.ai-review-message {
  margin-bottom: 6px;
}
.ai-review-summary {
  margin-top: 6px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}
.ai-review-list {
  margin-top: 6px;
  color: #e6a23c;
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
