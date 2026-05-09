<template>
  <div class="AddBox">
    <div class="BOX">
      <header class="add-activity-card-header">
        <h2>添加活动</h2>
      </header>
      <el-form ref="form" :model="form" label-width="0" :rules="rules" class="add-activity-form">
        <el-form-item prop="title">
          <el-input v-model="form.title" placeholder="活动标题"></el-input>
        </el-form-item>
        <el-form-item prop="quota">
          <el-input-number v-model="form.quota" :min="1" placeholder="活动名额"></el-input-number>
        </el-form-item>
        <el-form-item prop="oldId">
          <el-input v-model="form.oldId" placeholder="老人用户ID（user.id，非任意数字）"></el-input>
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="发布人电话"></el-input>
        </el-form-item>

        <div class="time-section add-activity-full-row">
          <div class="time-section-title">报名与活动时间</div>
          <div class="time-section-grid">
            <el-form-item prop="deadline">
              <el-date-picker
                v-model="form.deadline"
                type="datetime"
                placeholder="报名截止时间（晚于此不可报名）"
                align="right"
                style="width: 100%"
                :picker-options="pickerOptionsofform">
              </el-date-picker>
            </el-form-item>
            <el-form-item prop="date">
              <el-date-picker type="date" v-model="form.date" placeholder="活动举办日期" style="width: 100%"></el-date-picker>
            </el-form-item>
            <el-form-item prop="begin">
              <el-time-picker
                v-model="form.begin"
                placeholder="活动开始时间"
                style="width: 100%"
                @change="() => $refs.form && $refs.form.validateField('end')">
              </el-time-picker>
            </el-form-item>
            <el-form-item prop="end">
              <el-time-picker
                v-model="form.end"
                placeholder="活动结束时间"
                style="width: 100%"
                @change="() => $refs.form && $refs.form.validateField('end')">
              </el-time-picker>
            </el-form-item>
          </div>
        </div>

        <el-form-item prop="address">
          <el-input v-model="form.address" placeholder="活动地址"></el-input>
        </el-form-item>
        <el-form-item prop="description" class="add-activity-full-row">
          <el-input type="textarea" v-model="form.description" placeholder="活动描述"></el-input>
        </el-form-item>
        <el-alert
          class="add-activity-full-row"
          title="管理员发布的活动将默认为「审核通过」，志愿者端可报名。"
          type="info"
          :closable="false"
          show-icon
          style="width: 100%; margin-bottom: 12px;"
        />
        <el-form-item prop="message" class="add-activity-full-row">
          <el-input type="textarea" v-model="form.message" placeholder="管理员建议"></el-input>
        </el-form-item>
        <el-form-item class="add-activity-full-row add-activity-actions">
          <el-button class="add-activity-btn-cancel" @click="handleCancel">取 消</el-button>
          <el-button type="primary" class="add-activity-btn-submit" @click="onSubmitForm">提 交</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request';
import { format } from 'date-fns-tz';

export default {
  name: 'AdminView',
  data() {
    return {
      form: {},
      pickerOptionsofform: {
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
      rules: {
        title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
        quota: [{ required: true, message: '请输入活动名额', trigger: 'blur' }],
        deadline: [{ required: true, message: '请选择报名截止时间', trigger: 'change' }],
        date: [{ required: true, message: '请选择活动日期', trigger: 'change' }],
        begin: [
            { required: true, message: '请选择活动开始时间', trigger: 'change' },
            { validator: (rule, value, callback) => this.validateBeginAfterDeadline(rule, value, callback), trigger: ['change', 'blur'] }
        ],
        end: [
            { required: true, message: '请选择活动结束时间', trigger: 'change' },
            { validator: (rule, value, callback) => this.validateEndAfterBegin(rule, value, callback), trigger: ['change', 'blur'] }
        ],
        phone: [
            { required: true, message: '请输入发布人电话', trigger: 'blur' },
            { pattern: /^(\(\d{3,4}\)|\d{3,4}-|\s)?\d{7,14}$/, message: '请输入正确的电话号码', trigger: 'blur' }
        ],
        address: [{ required: true, message: '请输入活动地址', trigger: 'blur' }],
        oldId: [{ required: true, message: '请输入老人ID', trigger: 'blur' }],
        description: [{ required: true, message: '请输入活动描述', trigger: 'blur' }],
        message: [{ required: false, message: '请输入管理员建议', trigger: 'blur' }]
      }
    };
  },
  methods: {
    /** el-time-picker 多为 Date，也可能为字符串；统一成 HH:mm:ss */
    formatPickerTimeToHMS(val) {
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
      if (typeof val === 'string') {
        const m = val.trim().match(/^(\d{1,2}):(\d{2})(?::(\d{2}))?/);
        if (m) {
          return `${m[1].padStart(2, '0')}:${m[2]}:${(m[3] || '00').padStart(2, '0')}`;
        }
      }
      return '';
    },
    formatPickerDateToYMD(val) {
      if (!val) return '';
      const date = val instanceof Date ? val : new Date(val);
      if (isNaN(date.getTime())) return '';
      const y = date.getFullYear();
      const mo = String(date.getMonth() + 1).padStart(2, '0');
      const da = String(date.getDate()).padStart(2, '0');
      return `${y}-${mo}-${da}`;
    },
    validateBeginAfterDeadline(rule, value, callback) {
        const { form } = this;
        const dateStr = this.formatPickerDateToYMD(form.date);
        const hms = this.formatPickerTimeToHMS(value);
        if (!dateStr || !hms || !form.deadline) {
          callback();
          return;
        }
        const beginDt = new Date(`${dateStr} ${hms}`);
        const deadlineDt = form.deadline instanceof Date ? form.deadline : new Date(form.deadline);
        if (isNaN(beginDt.getTime()) || isNaN(deadlineDt.getTime())) {
          callback();
          return;
        }
        if (beginDt.getTime() <= deadlineDt.getTime()) {
            callback(new Error('活动开始时间必须晚于报名截止时间'));
        } else {
            callback();
        }
    },
    validateEndAfterBegin(rule, value, callback) {
        const dateStr = this.formatPickerDateToYMD(this.form.date);
        const beginHms = this.formatPickerTimeToHMS(this.form.begin);
        const endHms = this.formatPickerTimeToHMS(value);

        if (!value) {
            callback(new Error('请选择活动结束时间'));
            return;
        }
        if (!this.form.begin) {
            callback(new Error('请先选择活动开始时间'));
            return;
        }
        if (!dateStr || !beginHms || !endHms) {
            callback();
            return;
        }
        const beginDt = new Date(`${dateStr} ${beginHms}`);
        const endDt = new Date(`${dateStr} ${endHms}`);
        if (isNaN(beginDt.getTime()) || isNaN(endDt.getTime())) {
          callback();
          return;
        }
        if (endDt.getTime() <= beginDt.getTime()) {
            callback(new Error('活动结束时间必须晚于活动开始时间'));
        } else {
            callback();
        }
    },
    onSubmitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          const data = { ...this.form };
          // 格式化截止时间
          data.deadline = format(data.deadline, "yyyy-MM-dd'T'HH:mm:ss", { timeZone: 'Asia/Shanghai' });
          data.date = this.formatPickerDateToYMD(data.date);
          data.begin = this.formatPickerTimeToHMS(data.begin);
          data.end = this.formatPickerTimeToHMS(data.end);
          
          request.post('/administrator', data)
            .then(response => {
              if (response.code === 1) {
                this.$message.success(response.msg);
                this.resetForm();
              } else {
                this.$message.error(response.msg);
              }
            })
            .catch(error => {
              console.error('There was a problem with the request:', error);
            });
        } else {
          return false;
        }
      });
    },
    handleCancel() {
      this.$router.push('/adminView');
    },
    resetForm() {
      this.$refs.form.resetFields();
      this.form = {};
    }
  }
};
</script>

<style lang="scss" scoped>
.AddBox {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: 100%;
  box-sizing: border-box;
  padding: 32px 20px 48px;
  background: linear-gradient(165deg, #eef2f7 0%, #e4eaf2 45%, #f4f6f9 100%);

  .BOX {
    background-color: #fff;
    display: flex;
    flex-direction: column;
    align-items: stretch;
    width: 100%;
    max-width: 920px;
    border-radius: 16px;
    border: 1px solid rgba(228, 231, 237, 0.95);
    box-shadow:
      0 1px 2px rgba(15, 23, 42, 0.04),
      0 12px 40px rgba(15, 23, 42, 0.08);
    padding: 28px 32px 36px;
    box-sizing: border-box;

    .add-activity-card-header {
      margin-bottom: 20px;
      padding-bottom: 16px;
      border-bottom: 1px solid #ebeef5;

      h2 {
        margin: 0;
        font-size: 22px;
        font-weight: 600;
        color: #303133;
        letter-spacing: 0.02em;
        line-height: 1.3;
      }
    }

    .add-activity-form {
        width: 100%;
        padding: 4px 0 8px;
        display: flex;
        flex-flow: row wrap;
        justify-content: space-between;
        align-items: flex-start;

        ::v-deep .el-form-item {
          flex: 0 0 calc(50% - 10px);
          max-width: calc(50% - 10px);
          margin-right: 0;
          margin-bottom: 14px;
          box-sizing: border-box;

          &__label {
            display: none;
          }

          &__content {
            margin-left: 0 !important;
          }
        }

        ::v-deep .el-form-item.add-activity-full-row {
          flex: 0 0 100%;
          max-width: 100%;
        }

        > .add-activity-full-row {
          flex: 0 0 100%;
          width: 100%;
          max-width: 100%;
        }

        .el-input-number {
          width: 100%;
          .el-input__inner {
            text-align: left;
          }
        }

        ::v-deep .el-input__inner,
        ::v-deep .el-textarea__inner {
          border-radius: 8px;
        }

        .add-activity-actions {
          margin-top: 8px;
          margin-bottom: 0 !important;
          padding-top: 20px;
          border-top: 1px solid #ebeef5;

          ::v-deep .el-form-item__content {
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
          }

          ::v-deep .el-button {
            flex: 1 1 calc(50% - 6px);
            min-width: 120px;
            border-radius: 10px;
            padding: 12px 20px;
            font-weight: 500;
          }

          .add-activity-btn-cancel {
            border-color: #dcdfe6;
          }
        }
      }

      .time-section {
        width: 100%;
        margin: 8px 0 16px;
        padding: 18px 18px 10px;
        box-sizing: border-box;
        background: linear-gradient(180deg, #fafbfd 0%, #f5f7fa 100%);
        border: 1px solid #e4e7ed;
        border-radius: 12px;

        &-title {
          font-size: 15px;
          font-weight: 600;
          color: #303133;
          margin-bottom: 12px;
          padding-bottom: 8px;
          border-bottom: 1px solid #ebeef5;
        }

        .time-section-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 0 16px;
        }

        ::v-deep .el-form-item {
          flex: none;
          max-width: none;
          width: 100%;
          margin-bottom: 12px;
        }
      }

      @media (max-width: 768px) {
        max-width: 100%;
        padding: 22px 18px 28px;
        border-radius: 14px;

        .add-activity-card-header h2 {
          font-size: 20px;
        }

        .add-activity-form {
          ::v-deep .el-form-item {
            flex: 0 0 100%;
            max-width: 100%;
          }

          .add-activity-actions ::v-deep .el-button {
            flex: 1 1 100%;
          }
        }

        .time-section .time-section-grid {
          grid-template-columns: 1fr;
        }
      }
    }
}


</style>
