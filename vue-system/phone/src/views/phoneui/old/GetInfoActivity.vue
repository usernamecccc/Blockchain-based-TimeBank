<template>
    <div class="get-info-plain">
        <div class="ai-draft-box" :class="{ expanded: aiDraftOpen }">
            <div class="ai-draft-bar" @click="aiDraftOpen = !aiDraftOpen">
                <div>
                    <div class="ai-draft-title">AI发布助手</div>
                    <div class="ai-draft-subtitle">{{ aiDraft ? aiDraft.title : '用一句话生成活动草稿' }}</div>
                </div>
                <el-button size="mini" type="text">{{ aiDraftOpen ? '收起' : '展开' }}</el-button>
            </div>
            <div v-show="aiDraftOpen" class="ai-draft-content">
                <div class="ai-draft-input-row">
                    <el-input
                        v-model="aiDraftText"
                        size="small"
                        placeholder="例如：明天下午想找志愿者陪我去医院"
                        @keyup.enter.native="generateActivityDraft"
                    ></el-input>
                    <el-button size="small" type="primary" :loading="aiDraftLoading" @click="generateActivityDraft">生成</el-button>
                </div>
                <div v-if="aiDraft" class="ai-draft-result">
                    <div class="ai-draft-result-head">
                        <span>{{ aiDraft.title }}</span>
                        <el-button size="mini" type="primary" plain @click="applyActivityDraft">填入表单</el-button>
                    </div>
                    <div class="ai-draft-result-desc">{{ aiDraft.description }}</div>
                    <div v-if="compactDraftTips.length" class="ai-draft-tags">
                        <el-tag
                            v-for="item in compactDraftTips"
                            :key="item"
                            size="mini"
                            :type="draftWarningSet[item] ? 'warning' : 'info'"
                        >
                            {{ item }}
                        </el-tag>
                    </div>
                </div>
            </div>
        </div>
        <el-form ref="form" :model="form" label-width="0" :rules="rules">
            <el-form-item>
                <div class="field-hint">服务类型：{{ currentServiceTypeLabel }}</div>
            </el-form-item>
            <el-form-item prop="title">
                <el-input v-model="form.title" placeholder="活动标题"></el-input>
            </el-form-item>
            <el-form-item prop="quota">
                <el-input-number v-model="form.quota" :min="1" class="w-full-input-number"></el-input-number>
            </el-form-item>
            <el-form-item prop="volunteerReward">
                <div class="field-hint" v-if="volunteerRewardMaxCapped">每人答谢时间币（完成后支付，至多 {{ volunteerRewardDisplayMax }}）</div>
                <div class="field-hint" v-else>每人答谢时间币（完成后支付，0 表示不答谢）</div>
                <el-input-number
                    v-model="form.volunteerReward"
                    :min="0"
                    :max="volunteerRewardInputMax"
                    :precision="0"
                    class="w-full-input-number"
                ></el-input-number>
            </el-form-item>
            <div class="form-region">
                <div class="form-region__title">报名截止</div>
                <el-form-item prop="deadlineDay">
                    <el-date-picker
                        v-model="form.deadlineDay"
                        type="date"
                        placeholder="报名截止日期"
                        format="yyyy-MM-dd"
                        value-format="yyyy-MM-dd"
                        class="w-full-picker"
                        :picker-options="{ shortcuts: pickerDateShortcuts }"
                        @change="queueValidateDeadlineRelated">
                    </el-date-picker>
                </el-form-item>
                <div class="pair-line">
                    <el-form-item prop="deadlineHour" class="pair-cell">
                        <el-select v-model="form.deadlineHour" placeholder="截止时间：时" class="w-full-select" size="medium" @change="queueValidateDeadlineRelated">
                            <el-option v-for="h in hourOptions" :key="'dh' + h" :label="formatHourLabel(h)" :value="h" />
                        </el-select>
                    </el-form-item>
                    <el-form-item prop="deadlineMinute" class="pair-cell">
                        <el-select v-model="form.deadlineMinute" placeholder="截止时间：分" class="w-full-select" size="medium" @change="queueValidateDeadlineRelated">
                            <el-option v-for="m in quarterMinuteOptions" :key="'dm' + m" :label="formatMinuteLabel(m)" :value="m" />
                        </el-select>
                    </el-form-item>
                </div>
            </div>
            <div class="form-region">
                <div class="form-region__title">活动时间</div>
                <el-form-item prop="date">
                    <el-date-picker type="dates" v-model="form.dates" placeholder="活动日期(可多选)" class="w-full-picker"></el-date-picker>
                </el-form-item>
                <div class="pair-line">
                    <el-form-item prop="beginHour" class="pair-cell">
                        <el-select v-model="form.beginHour" placeholder="开始时间：时" class="w-full-select" size="medium" @change="queueValidateBeginEnd">
                            <el-option v-for="h in hourOptions" :key="'bh' + h" :label="formatHourLabel(h)" :value="h" />
                        </el-select>
                    </el-form-item>
                    <el-form-item prop="beginMinute" class="pair-cell">
                        <el-select v-model="form.beginMinute" placeholder="开始时间：分" class="w-full-select" size="medium" @change="queueValidateBeginEnd">
                            <el-option v-for="m in quarterMinuteOptions" :key="'bm' + m" :label="formatMinuteLabel(m)" :value="m" />
                        </el-select>
                    </el-form-item>
                </div>
                <div class="pair-line">
                    <el-form-item prop="endHour" class="pair-cell">
                        <el-select v-model="form.endHour" placeholder="结束时间：时" class="w-full-select" size="medium" @change="queueValidateBeginEnd">
                            <el-option v-for="h in hourOptions" :key="'eh' + h" :label="formatHourLabel(h)" :value="h" />
                        </el-select>
                    </el-form-item>
                    <el-form-item prop="endMinute" class="pair-cell">
                        <el-select v-model="form.endMinute" placeholder="结束时间：分" class="w-full-select" size="medium" @change="queueValidateBeginEnd">
                            <el-option v-for="m in quarterMinuteOptions" :key="'em' + m" :label="formatMinuteLabel(m)" :value="m" />
                        </el-select>
                    </el-form-item>
                </div>
            </div>
            <el-form-item prop="description">
                <el-input type="textarea" v-model="form.description" placeholder="活动描述"></el-input>
            </el-form-item>
            <el-form-item v-for="field in dynamicExtraFields" :key="field.key">
                <div class="field-hint">{{ field.label }}<span v-if="field.required">（必填）</span></div>
                <el-input
                    v-if="field.type === 'textarea'"
                    type="textarea"
                    :placeholder="field.placeholder"
                    v-model.trim="form.extra[field.key]"
                ></el-input>
                <el-select
                    v-else-if="field.type === 'boolean'"
                    v-model="form.extra[field.key]"
                    placeholder="请选择"
                    class="w-full-select"
                >
                    <el-option label="是" :value="true"></el-option>
                    <el-option label="否" :value="false"></el-option>
                </el-select>
                <el-input
                    v-else
                    :placeholder="field.placeholder"
                    v-model.trim="form.extra[field.key]"
                ></el-input>
            </el-form-item>
            <el-form-item class="form-btn-row">
                <el-button @click="handleCancel">取 消</el-button>
                <el-button type="primary" @click="onSubmitForm">确 定</el-button>
            </el-form-item>
        </el-form>
    </div>
</template>

<script>
import { format } from 'date-fns-tz';
import request from '@/utils/request';

/** 只能选择 15 分钟档：下拉分钟取值（仅四种） */
const QUARTER_MINUTES = Object.freeze([0, 15, 30, 45]);
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

function emptyFormModel() {
    return {
        serviceType: 'other_service',
        title: '',
        quota: 1,
        deadlineDay: null,
        deadlineHour: null,
        deadlineMinute: null,
        dates: [],
        beginHour: null,
        beginMinute: null,
        endHour: null,
        endMinute: null,
        description: '',
        volunteerReward: 0,
        extra: {},
    };
}

/** 下拉「分」是否为未选择（0 是合法值） */
function isMinuteUnset(v) {
    return v === null || v === undefined;
}

export default {
    name: 'AddActivityOld',
    data() {
        const hourOpts = [...Array(24).keys()];
        const quarterShortcuts = [{
            text: '今天',
            onClick(picker) {
                const d = new Date();
                d.setHours(0, 0, 0, 0);
                picker.$emit('pick', d);
            }
        }];
        return {
            hourOptions: hourOpts,
            quarterMinuteOptions: [...QUARTER_MINUTES],
            pickerDateShortcuts: quarterShortcuts.concat([{
                text: '明天',
                onClick(picker) {
                    const d = new Date();
                    d.setHours(0, 0, 0, 0);
                    d.setDate(d.getDate() + 1);
                    picker.$emit('pick', d);
                }
            }]),
            form: emptyFormModel(),
            aiDraftText: '',
            aiDraft: null,
            aiDraftLoading: false,
            aiDraftOpen: false,
            volunteerRewardMaxCapped: true,
            volunteerRewardDisplayMax: '999',
            rules: {
                title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
                quota: [{ required: true, message: '请输入活动名额', trigger: 'blur' }],
                deadlineDay: [{ required: true, message: '请选择报名截止日期', trigger: 'change' }],
                deadlineHour: [
                    {
                        validator: (_, v, cb) => {
                            if (v === null || v === undefined || v === '') {
                                cb(new Error('请选择截止时间：时'));
                            } else {
                                cb();
                            }
                        },
                        trigger: 'change'
                    },
                ],
                deadlineMinute: [
                    {
                        validator: (_, v, cb) => {
                            if (isMinuteUnset(v)) {
                                cb(new Error('请选择截止时间：分'));
                                return;
                            }
                            if (!QUARTER_MINUTES.includes(v)) {
                                cb(new Error('截止时间：分须为 15 分钟一档（00 / 15 / 30 / 45）'));
                                return;
                            }
                            cb();
                        },
                        trigger: 'change'
                    },
                ],
                dates: [{ required: true, message: '请选择活动日期(可多选)', trigger: 'change' }],
                beginHour: [
                    {
                        validator: (_, v, cb) => {
                            if (v === null || v === undefined || v === '') {
                                cb(new Error('请选择开始时间：时'));
                            } else {
                                cb();
                            }
                        },
                        trigger: 'change'
                    },
                    { validator: (rule, val, cb) => this.validateBeginAfterDeadline(rule, val, cb), trigger: 'change' },
                ],
                beginMinute: [
                    {
                        validator: (_, v, cb) => {
                            if (isMinuteUnset(v)) {
                                cb(new Error('请选择开始时间：分'));
                                return;
                            }
                            if (!QUARTER_MINUTES.includes(v)) {
                                cb(new Error('开始时间：分须为 15 分钟一档（00 / 15 / 30 / 45）'));
                                return;
                            }
                            cb();
                        },
                        trigger: 'change'
                    },
                    { validator: (rule, val, cb) => this.validateBeginAfterDeadline(rule, val, cb), trigger: 'change' },
                ],
                endHour: [
                    {
                        validator: (_, v, cb) => {
                            if (v === null || v === undefined || v === '') {
                                cb(new Error('请选择结束时间：时'));
                            } else {
                                cb();
                            }
                        },
                        trigger: 'change'
                    },
                    { validator: (rule, val, cb) => this.validateEndAfterBegin(rule, val, cb), trigger: 'change' },
                ],
                endMinute: [
                    {
                        validator: (_, v, cb) => {
                            if (isMinuteUnset(v)) {
                                cb(new Error('请选择结束时间：分'));
                                return;
                            }
                            if (!QUARTER_MINUTES.includes(v)) {
                                cb(new Error('结束时间：分须为 15 分钟一档（00 / 15 / 30 / 45）'));
                                return;
                            }
                            cb();
                        },
                        trigger: 'change'
                    },
                    { validator: (rule, val, cb) => this.validateEndAfterBegin(rule, val, cb), trigger: 'change' },
                ],
                phone: [
                    { required: true, message: '请输入发布人电话', trigger: 'blur' },
                    { pattern: /^(\(\d{3,4}\)|\d{3,4}-|\s)?\d{7,14}$/, message: '请输入正确的电话号码', trigger: 'blur' }
                ],
                description: [{ required: true, message: '请输入活动描述', trigger: 'blur' }],
                volunteerReward: [
                    {
                        validator: (_, val, cb) => {
                            if (val === null || val === undefined || val === '') {
                                cb();
                                return;
                            }
                            const n = Number(val);
                            if (!Number.isFinite(n) || n < 0) {
                                cb(new Error('答谢时间币须为非负数'));
                                return;
                            }
                            if (this.volunteerRewardMaxCapped) {
                                const cap = Number(this.volunteerRewardDisplayMax);
                                if (Number.isFinite(cap) && n > cap) {
                                    cb(new Error(`答谢时间币不能超过 ${cap}`));
                                    return;
                                }
                            }
                            cb();
                        },
                        trigger: 'change',
                    },
                ],
            }
        };
    },
    created() {
        this.initServiceTypeFromRoute();
        this.ensureDynamicExtraShape();
    },
    computed: {
        currentServiceTypeLabel() {
            return SERVICE_TYPE_LABELS[this.form.serviceType] || SERVICE_TYPE_LABELS.other_service;
        },
        dynamicExtraFields() {
            return SERVICE_TYPE_FIELD_CONFIG[this.form.serviceType] || SERVICE_TYPE_FIELD_CONFIG.other_service;
        },
        compactDraftTips() {
            if (!this.aiDraft) return [];
            const suggestions = Array.isArray(this.aiDraft.suggestions) ? this.aiDraft.suggestions.slice(0, 2) : [];
            const warnings = Array.isArray(this.aiDraft.warnings) ? this.aiDraft.warnings.slice(0, 1) : [];
            return suggestions.concat(warnings);
        },
        draftWarningSet() {
            const map = {};
            if (this.aiDraft && Array.isArray(this.aiDraft.warnings)) {
                this.aiDraft.warnings.forEach((item) => {
                    map[item] = true;
                });
            }
            return map;
        },
        volunteerRewardInputMax() {
            if (!this.volunteerRewardMaxCapped) {
                return 999999;
            }
            const n = parseInt(String(this.volunteerRewardDisplayMax), 10);
            return Number.isFinite(n) && n > 0 ? n : 999999;
        },
    },
    mounted() {
        request
            .get('/info/publishActivityFee')
            .then((res) => {
                if (res.code === 1 && res.data && res.data.volunteerRewardMaxCapped === false) {
                    this.volunteerRewardMaxCapped = false;
                } else if (res.code === 1 && res.data && res.data.volunteerRewardMax) {
                    this.volunteerRewardDisplayMax = String(res.data.volunteerRewardMax);
                    this.volunteerRewardMaxCapped = true;
                }
            })
            .catch(() => {});
    },
    methods: {
        initServiceTypeFromRoute() {
            const routeType = this.$route && this.$route.query ? this.$route.query.serviceType : '';
            this.form.serviceType = SERVICE_TYPE_LABELS[routeType] ? routeType : 'other_service';
        },
        ensureDynamicExtraShape() {
            if (!this.form.extra || typeof this.form.extra !== 'object') {
                this.$set(this.form, 'extra', {});
            }
            this.dynamicExtraFields.forEach((field) => {
                if (Object.prototype.hasOwnProperty.call(this.form.extra, field.key)) {
                    return;
                }
                this.$set(this.form.extra, field.key, field.type === 'boolean' ? null : '');
            });
        },
        validateDynamicExtraFields() {
            this.ensureDynamicExtraShape();
            for (const field of this.dynamicExtraFields) {
                if (!field.required) continue;
                const val = this.form.extra[field.key];
                if (field.type === 'boolean') {
                    if (val === null || val === undefined) {
                        this.$message.error(`请填写：${field.label}`);
                        return false;
                    }
                    continue;
                }
                if (val === null || val === undefined || String(val).trim() === '') {
                    this.$message.error(`请填写：${field.label}`);
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
                    if (val !== null && val !== undefined) {
                        payload[field.key] = val;
                    }
                    return;
                }
                if (val !== null && val !== undefined && String(val).trim() !== '') {
                    payload[field.key] = val;
                }
            });
            return payload;
        },
        formatHourLabel(h) {
            return `${String(h).padStart(2, '0')} 点`;
        },
        formatMinuteLabel(m) {
            return `${String(m).padStart(2, '0')} 分`;
        },
        /** 截止日期 + 时起止：value-format yyyy-MM-dd 或 Date */
        composeDeadlineDatetime() {
            const { deadlineDay, deadlineHour, deadlineMinute } = this.form;
            if (!deadlineDay || deadlineHour == null || deadlineMinute == null || !QUARTER_MINUTES.includes(deadlineMinute)) {
                return null;
            }
            const ds = deadlineDay instanceof Date
                ? this.formatDayToYmd(deadlineDay)
                : String(deadlineDay).trim();
            if (!ds) return null;
            const [y, mo, da] = ds.split('-').map((x) => parseInt(x, 10));
            if ([y, mo, da].some((x) => Number.isNaN(x))) return null;
            const d = new Date(y, mo - 1, da);
            if (Number.isNaN(d.getTime())) return null;
            d.setHours(deadlineHour, deadlineMinute, 0, 0);
            return d;
        },
        formatDayToYmd(d) {
            const dd = new Date(d);
            if (Number.isNaN(dd.getTime())) return '';
            const y = dd.getFullYear();
            const m = String(dd.getMonth() + 1).padStart(2, '0');
            const day = String(dd.getDate()).padStart(2, '0');
            return `${y}-${m}-${day}`;
        },
        composeWallClockMinutes(hour, minute) {
            if (hour == null || minute == null || !QUARTER_MINUTES.includes(minute)) return null;
            return hour * 60 + minute;
        },
        composeActivityBeginEndDate() {
            const { dates } = this.form;
            if (!dates || dates.length === 0) return null;
            const firstTs = Math.min(...dates.map((dt) => new Date(dt).getTime()));
            return new Date(firstTs);
        },
        queueValidateDeadlineRelated() {
            this.$nextTick(() => {
                const f = this.$refs.form;
                if (!f) return;
                ['deadlineDay', 'deadlineHour', 'deadlineMinute', 'beginHour', 'beginMinute'].forEach((n) => f.validateField(n));
            });
        },
        queueValidateBeginEnd() {
            this.$nextTick(() => {
                const f = this.$refs.form;
                if (!f) return;
                ['beginHour', 'beginMinute', 'endHour', 'endMinute'].forEach((n) => f.validateField(n));
            });
        },
        validateBeginAfterDeadline(rule, value, callback) {
            const { form } = this;
            if (!form.dates || form.dates.length === 0 || form.beginHour == null || form.beginMinute == null) {
                callback();
                return;
            }

            const deadlineDt = this.composeDeadlineDatetime();
            if (!deadlineDt) {
                callback();
                return;
            }

            const dayBasis = this.composeActivityBeginEndDate();
            if (!dayBasis) {
                callback();
                return;
            }

            const beginDate = new Date(dayBasis.getTime());
            beginDate.setHours(form.beginHour, form.beginMinute, 0, 0);
            if (beginDate <= deadlineDt) {
                callback(new Error('开始时间须整体晚于报名截止时间'));
            } else {
                callback();
            }
        },
        validateEndAfterBegin(rule, value, callback) {
            const { form } = this;

            const beginMinutes = this.composeWallClockMinutes(form.beginHour, form.beginMinute);
            const endMinutes = this.composeWallClockMinutes(form.endHour, form.endMinute);

            if (beginMinutes === null || endMinutes === null) {
                callback();
                return;
            }

            if (endMinutes <= beginMinutes) {
                callback(new Error('结束时间须晚于开始时间'));
            } else {
                callback();
            }
        },
        onSubmitForm() {
            this.$refs.form.validate((valid) => {
                if (valid) {
                    if (!this.validateDynamicExtraFields()) {
                        return;
                    }
                    const deadlineDt = this.composeDeadlineDatetime();
                    const dayBasis = this.composeActivityBeginEndDate();
                    if (!deadlineDt || !dayBasis) {
                        this.$message.error('请补全「报名截止」日期与时间及「活动时间」各项');
                        return;
                    }

                    const beginDt = new Date(dayBasis.getTime());
                    beginDt.setHours(this.form.beginHour, this.form.beginMinute, 0, 0);

                    const endDt = new Date(dayBasis.getTime());
                    endDt.setHours(this.form.endHour, this.form.endMinute, 0, 0);

                    const formatTimeString = (dt) => {
                        if (!(dt instanceof Date) || Number.isNaN(dt.getTime())) return '';
                        const hh = dt.getHours().toString().padStart(2, '0');
                        const mi = dt.getMinutes().toString().padStart(2, '0');
                        const ss = dt.getSeconds().toString().padStart(2, '0');
                        return `${hh}:${mi}:${ss}`;
                    };

                    const formatDateString = (dateStr) => {
                        if (!dateStr) return '';
                        const dt = dateStr instanceof Date ? dateStr : new Date(dateStr);
                        const year = dt.getFullYear();
                        const month = dt.getMonth() + 1 < 10 ? `0${dt.getMonth() + 1}` : dt.getMonth() + 1;
                        const day = dt.getDate() < 10 ? `0${dt.getDate()}` : dt.getDate();
                        return `${year}-${month}-${day}`;
                    };

                    const data = { ...this.form };
                    data.begin = formatTimeString(beginDt);
                    data.end = formatTimeString(endDt);
                    const normalizedDates = (data.dates || []).map((item) => formatDateString(item)).sort();
                    data.dates = normalizedDates;
                    data.date = normalizedDates[0] || '';
                    data.message = JSON.stringify({ dates: normalizedDates });
                    data.serviceType = this.form.serviceType || 'other_service';
                    data.extraJson = JSON.stringify(this.buildExtraPayload());
                    data.deadline = format(deadlineDt, "yyyy-MM-dd'T'HH:mm:ss", { timeZone: 'Asia/BeiJing' });
                    data.phone = '15245678901';

                    delete data.deadlineDay;
                    delete data.deadlineHour;
                    delete data.deadlineMinute;
                    delete data.beginHour;
                    delete data.beginMinute;
                    delete data.endHour;
                    delete data.endMinute;

                    localStorage.setItem('form', JSON.stringify(data));
                    this.$emit('next');
                    this.$router.push('/locationGet');
                    this.resetForm();
                }
            });
        },
        generateActivityDraft() {
            const text = (this.aiDraftText || '').trim();
            if (!text) {
                this.$message.warning('请先描述你的服务需求');
                return;
            }
            this.aiDraftLoading = true;
            request.post('/users/old/ai/activity-draft', { text }, { timeout: 150000 })
                .then((response) => {
                    if (response.code === 1 && response.data) {
                        this.aiDraft = response.data;
                        this.$message.success('已生成活动草稿');
                    } else {
                        this.$message.error(response.msg || 'AI草稿生成失败');
                    }
                })
                .catch(() => {
                    this.$message.error('AI服务暂时不可用');
                })
                .finally(() => {
                    this.aiDraftLoading = false;
                });
        },
        applyActivityDraft() {
            if (!this.aiDraft) return;
            if (this.aiDraft.title) this.form.title = this.aiDraft.title;
            if (this.aiDraft.description) this.form.description = this.aiDraft.description;
            if (this.aiDraft.quota) this.form.quota = Number(this.aiDraft.quota);
            if (this.aiDraft.volunteerReward !== undefined && this.aiDraft.volunteerReward !== null) {
                this.form.volunteerReward = Number(this.aiDraft.volunteerReward);
            }
            this.$message.success('已填入表单，请继续补充时间和地址');
        },
        handleCancel() {
            this.$router.push('/serverOld');
        },
        resetForm() {
            const serviceType = this.form.serviceType || 'other_service';
            this.form = emptyFormModel();
            this.form.serviceType = SERVICE_TYPE_LABELS[serviceType] ? serviceType : 'other_service';
            this.ensureDynamicExtraShape();
            this.$nextTick(() => {
                if (this.$refs.form) {
                    this.$refs.form.resetFields();
                }
            });
        },
    },
};
</script>

<style lang="scss" scoped>
.get-info-plain {
    padding: 20px;
}

.ai-draft-box {
    padding: 0;
    margin-bottom: 16px;
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    background: #ffffff;
    box-sizing: border-box;
    overflow: hidden;
}

.ai-draft-box.expanded {
    box-shadow: 0 2px 8px rgba(31, 45, 61, 0.08);
}

.ai-draft-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 12px;
    cursor: pointer;
}

.ai-draft-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
}

.ai-draft-subtitle {
    margin-top: 2px;
    font-size: 12px;
    color: #909399;
}

.ai-draft-content {
    padding: 0 12px 12px;
}

.ai-draft-input-row {
    display: flex;
    gap: 8px;
    align-items: center;
}

.ai-draft-result {
    margin-top: 10px;
    padding: 8px;
    border-radius: 8px;
    background: #f5f7fb;
    font-size: 13px;
    line-height: 1.5;
}

.ai-draft-result-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    font-weight: 600;
    color: #303133;
}

.ai-draft-result-desc {
    margin-top: 4px;
    color: #606266;
}

.ai-draft-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 8px;
}

.field-hint {
    font-size: 12px;
    color: #909399;
    margin-bottom: 6px;
    line-height: 1.4;
}

.form-region {
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    padding: 12px 12px 4px;
    margin-bottom: 16px;
    background: #fafafa;
    box-sizing: border-box;

    &__title {
        font-size: 15px;
        font-weight: 600;
        color: #303133;
        margin: 0 0 10px;
        padding-bottom: 8px;
        border-bottom: 1px solid #ebeef5;
    }

    &::v-deep .el-form-item {
        margin-bottom: 12px;
    }

    .pair-line {
        margin-bottom: 12px;
    }

    .pair-line:last-child {
        margin-bottom: 0;
    }
}

.w-full-picker {
    width: 100% !important;
}

.w-full-select {
    width: 100%;
}

.w-full-input-number {
    width: 100%;

    &::v-deep .el-input__inner {
        text-align: left;
    }
}

.pair-line {
    display: flex;
    gap: 10px;
    align-items: flex-start;
    margin-bottom: 18px;

    &::v-deep .el-form-item {
        margin-bottom: 0;
    }
}

.pair-cell {
    flex: 1;
    min-width: 0;
    margin-bottom: 0 !important;
}

.form-btn-row::v-deep .el-form-item__content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-left: 0 !important;
}

.form-btn-row::v-deep .el-button {
    width: 48%;
}

.get-info-plain {
    min-height: calc(100vh - 68px);
    padding: 16px 14px 24px;
    background: var(--old-bg);
    color: var(--old-text);
}

.ai-draft-box {
    border-color: var(--old-border);
    border-radius: 14px;
    background: var(--old-surface);
    box-shadow: 0 5px 16px rgba(111, 76, 43, 0.10);
}

.ai-draft-bar {
    padding: 14px;
    min-height: 58px;
}

.ai-draft-title {
    color: var(--old-primary-strong);
    font-size: 17px;
}

.ai-draft-subtitle {
    color: var(--old-muted);
    font-size: 14px;
    line-height: 1.45;
}

.ai-draft-input-row {
    align-items: stretch;
}

.ai-draft-input-row ::v-deep .el-input__inner,
.get-info-plain ::v-deep .el-input__inner,
.get-info-plain ::v-deep .el-textarea__inner {
    min-height: 48px;
    border-color: var(--old-border);
    border-radius: 10px;
    font-size: 16px;
    line-height: 1.5;
}

.get-info-plain ::v-deep .el-textarea__inner {
    min-height: 112px !important;
}

.get-info-plain ::v-deep .el-button {
    min-height: 46px;
    border-radius: 10px;
    font-size: 16px;
    font-weight: 700;
}

.get-info-plain ::v-deep .el-button--primary {
    background: var(--old-primary);
    border-color: var(--old-primary);
}

.ai-draft-result {
    border: 1px solid #f0d5af;
    background: #e8f4f8;
    font-size: 15px;
}

.form-region {
    border-color: var(--old-border);
    border-radius: 14px;
    background: var(--old-surface);
}

.form-region__title {
    color: var(--old-primary-strong);
    font-size: 17px;
}

.field-hint {
    color: var(--old-muted);
    font-size: 14px;
}

.form-btn-row::v-deep .el-button {
    min-height: 52px;
}
</style>
