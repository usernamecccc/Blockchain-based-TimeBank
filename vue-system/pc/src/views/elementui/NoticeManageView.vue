<template>
  <div class="notice-page">
    <div class="toolbar">
      <el-button type="primary" icon="el-icon-plus" round @click="openCreate">发布公告</el-button>
      <el-button icon="el-icon-refresh" round @click="loadList">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%; margin-top: 16px;">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
      <el-table-column prop="content" label="正文" min-width="260" show-overflow-tooltip />
      <el-table-column prop="createTime" label="发布时间" width="170" />
      <el-table-column label="操作" width="160" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="openEdit(scope.row)">编辑</el-button>
          <el-button type="text" size="small" style="color: #f56c6c" @click="removeRow(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="560px"
      :append-to-body="true"
      :modal-append-to-body="false"
      :close-on-click-modal="false"
      custom-class="notice-dialog"
      @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="可选，简短标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="公告正文（必填）"
            maxlength="4000"
            show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保 存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request';

export default {
  name: 'NoticeManageView',
  data() {
    return {
      loading: false,
      tableData: [],
      dialogVisible: false,
      dialogTitle: '发布公告',
      submitting: false,
      form: {
        id: null,
        title: '',
        content: '',
      },
      rules: {
        content: [{ required: true, message: '请输入公告正文', trigger: 'blur' }],
      },
    };
  },
  mounted() {
    this.loadList();
  },
  methods: {
    loadList() {
      this.loading = true;
      request
        .get('/administrator/notices')
        .then(res => {
          if (res.code === 1) {
            this.tableData = res.data || [];
          } else {
            this.$message.error(res.msg || '加载失败');
          }
        })
        .catch(() => this.$message.error('网络错误'))
        .finally(() => {
          this.loading = false;
        });
    },
    openCreate() {
      this.dialogTitle = '发布公告';
      this.form = { id: null, title: '', content: '' };
      this.dialogVisible = true;
      this.$nextTick(() => this.$refs.formRef && this.$refs.formRef.clearValidate());
    },
    openEdit(row) {
      this.dialogTitle = '编辑公告';
      this.form = {
        id: row.id,
        title: row.title || '',
        content: row.content || '',
      };
      this.dialogVisible = true;
      this.$nextTick(() => this.$refs.formRef && this.$refs.formRef.clearValidate());
    },
    resetForm() {
      this.form = { id: null, title: '', content: '' };
    },
    submit() {
      const formRef = this.$refs.formRef;
      if (!formRef) return;
      formRef.validate(valid => {
        if (!valid) return;
        this.submitting = true;
        const payload = {
          title: (this.form.title || '').trim(),
          content: (this.form.content || '').trim(),
        };
        const req = this.form.id
          ? request.put('/administrator/notices', { id: this.form.id, ...payload })
          : request.post('/administrator/notices', payload);
        req
          .then(res => {
            if (res.code === 1) {
              this.$message.success('保存成功');
              this.dialogVisible = false;
              this.loadList();
            } else {
              this.$message.error(res.msg || '保存失败');
            }
          })
          .catch(() => this.$message.error('网络错误'))
          .finally(() => {
            this.submitting = false;
          });
      });
    },
    removeRow(row) {
      const hint = row.title || (row.content && row.content.slice(0, 20)) || `#${row.id}`;
      this.$confirm(`确定删除公告「${hint}」？`, '提示', { type: 'warning' })
        .then(() => request.delete(`/administrator/notices/${row.id}`))
        .then(res => {
          if (res.code === 1) {
            this.$message.success('已删除');
            this.loadList();
          } else {
            this.$message.error(res.msg || '删除失败');
          }
        })
        .catch(err => {
          if (err !== 'cancel') this.$message.error('删除失败');
        });
    },
  },
};
</script>

<style scoped lang="scss">
.notice-page {
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  min-height: 360px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
