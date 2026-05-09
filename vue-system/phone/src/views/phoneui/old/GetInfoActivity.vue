<template>
    <div class="get-info-plain">
        <el-form ref="form" :model="form" label-width="0" :rules="rules">
            <el-form-item prop="title">
                <el-input v-model="form.title" placeholder="活动标题"></el-input>
            </el-form-item>
            <el-form-item prop="quota">
                <el-input-number v-model="form.quota" :min="1" class="w-full-input-number"></el-input-number>
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
            <el-form-item class="form-btn-row">
                <el-button @click="handleCancel">取 消</el-button>
                <el-button type="primary" @click="onSubmitForm">确 定</el-button>
            </el-form-item>
        </el-form>
    </div>
</template>

<script>
import { format } from 'date-fns-tz';

/** 只能选择 15 分钟档：下拉分钟取值（仅四种） */
const QUARTER_MINUTES = Object.freeze([0, 15, 30, 45]);

function emptyFormModel() {
    return {
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
            }
        };
    },
    methods: {
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
        handleCancel() {
            this.$router.push('/serverOld');
        },
        resetForm() {
            this.form = emptyFormModel();
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
</style>
