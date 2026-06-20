<template>
  <div class="sign-in-page">
    <div class="activity-brief" v-if="activityTitle">
      <div class="activity-brief-title">{{ activityTitle }}</div>
      <div class="activity-brief-address" v-if="activityAddress">
        <i class="el-icon-location-outline"></i>
        {{ activityAddress }}
      </div>
    </div>

    <div class="map-panel">
      <div id="container" class="map-container"></div>
      <div v-if="mapLoading" class="map-overlay">
        <i class="el-icon-loading"></i>
        <span>正在定位…</span>
      </div>
      <div v-else-if="mapError" class="map-overlay map-overlay--error">
        <i class="el-icon-warning-outline"></i>
        <span>{{ mapError }}</span>
        <el-button size="mini" type="primary" round @click="refreshLocation">重新定位</el-button>
      </div>
    </div>

    <div class="location-card">
      <div class="location-row">
        <span class="label">当前位置</span>
        <el-button type="text" icon="el-icon-refresh" @click="refreshLocation" :loading="mapLoading">刷新</el-button>
      </div>
      <div class="location-address">{{ address || '定位中，请稍候…' }}</div>
      <div class="location-coords" v-if="latitude != null">
        经纬度：{{ latitude.toFixed(6) }}, {{ longitude.toFixed(6) }}
      </div>
      <div class="location-tip">
        <i class="el-icon-info"></i>
        请在活动地点附近（约 100 米内）完成签到
      </div>
    </div>

    <el-button
      type="primary"
      round
      class="sign-btn"
      :loading="signingIn"
      :disabled="!canSignIn"
      @click="signIn"
    >
      {{ signingIn ? '签到中…' : '确认签到' }}
    </el-button>
  </div>
</template>

<script>
import AMapLoader from '@amap/amap-jsapi-loader';
import request from '@/utils/request';

const AMAP_KEY = 'cc996da095298e80e521e0df7a3d8b38';
const AMAP_SECURITY = '0f44724be842da40ac3db0cc34868b70';

export default {
  name: 'SignInUser',
  data() {
    return {
      map: null,
      userMarker: null,
      AMapRef: null,
      address: '',
      latitude: null,
      longitude: null,
      id: null,
      activityTitle: '',
      activityAddress: '',
      mapLoading: true,
      mapError: '',
      signingIn: false,
    };
  },
  computed: {
    canSignIn() {
      return !!this.address && !this.mapLoading && !this.mapError && !this.signingIn;
    },
  },
  created() {
    this.id = parseInt(this.$route.query.id, 10);
    this.activityTitle = this.$route.query.title || '';
    this.activityAddress = this.$route.query.address || '';
    this.fetchActivityDetail();
  },
  mounted() {
    this.injectAmapSecurity();
    this.initAMap();
  },
  beforeDestroy() {
    this.map?.destroy();
  },
  methods: {
    injectAmapSecurity() {
      if (window._AMapSecurityConfig) return;
      const securityScript = document.createElement('script');
      securityScript.type = 'text/javascript';
      securityScript.innerHTML = `
        window._AMapSecurityConfig = {
          securityJsCode: '${AMAP_SECURITY}'
        };
      `;
      document.body.appendChild(securityScript);
    },
    fetchActivityDetail() {
      if (!this.id) return;
      const params = new URLSearchParams({ pageSize: '1', page: '1', id: String(this.id) });
      request.get(`users/volold/activity?${params}`)
        .then((response) => {
          if (response.code === 1 && response.data) {
            this.activityTitle = response.data.title || this.activityTitle;
            this.activityAddress = response.data.address || this.activityAddress;
          }
        })
        .catch(() => {});
    },
    refreshLocation() {
      this.mapLoading = true;
      this.mapError = '';
      this.address = '';
      this.locateAndRender();
    },
    initAMap() {
      AMapLoader.load({
        key: AMAP_KEY,
        version: '1.4.15',
        plugins: ['AMap.Scale', 'AMap.Geocoder', 'AMap.ToolBar'],
      })
        .then((AMap) => {
          this.AMapRef = AMap;
          this.map = new AMap.Map('container', {
            viewMode: '2D',
            zoom: 16,
            center: [116.397428, 39.90923],
          });
          this.map.addControl(new AMap.Scale());
          this.map.addControl(new AMap.ToolBar({ locate: false }));
          this.locateAndRender();
        })
        .catch(() => {
          this.mapLoading = false;
          this.mapError = '地图加载失败，请检查网络后重试';
        });
    },
    locateAndRender() {
      if (!navigator.geolocation) {
        this.mapLoading = false;
        this.mapError = '当前浏览器不支持定位';
        return;
      }
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const { latitude, longitude } = position.coords;
          this.latitude = latitude;
          this.longitude = longitude;
          this.updateUserMarker(longitude, latitude);
          this.reverseGeocode(longitude, latitude);
        },
        (error) => {
          this.mapLoading = false;
          const messages = {
            1: '定位被拒绝，请在浏览器设置中允许位置权限',
            2: '暂时无法获取位置，请稍后重试',
            3: '定位超时，请点击刷新重试',
          };
          this.mapError = messages[error.code] || '获取位置失败，请重试';
        },
        { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 }
      );
    },
    updateUserMarker(longitude, latitude) {
      if (!this.map || !this.AMapRef) return;
      const position = [longitude, latitude];
      this.map.setCenter(position);
      this.map.setZoom(17);
      if (this.userMarker) {
        this.userMarker.setPosition(position);
      } else {
        this.userMarker = new this.AMapRef.Marker({
          position,
          title: '我的位置',
        });
        this.map.add(this.userMarker);
      }
    },
    reverseGeocode(longitude, latitude) {
      if (!this.AMapRef) return;
      this.AMapRef.plugin('AMap.Geocoder', () => {
        const geocoder = new this.AMapRef.Geocoder({ city: null });
        geocoder.getAddress([longitude, latitude], (status, result) => {
          this.mapLoading = false;
          if (status === 'complete' && result.info === 'OK') {
            this.address = result.regeocode.formattedAddress;
            this.mapError = '';
          } else {
            this.address = `${latitude.toFixed(6)}, ${longitude.toFixed(6)}`;
            this.$message.warning('地址解析失败，已使用坐标作为签到位置');
          }
        });
      });
    },
    signIn() {
      if (!this.canSignIn) return;
      this.signingIn = true;
      request.put('users/vol/sign', { id: this.id, address: this.address })
        .then((response) => {
          if (response.code === 1) {
            this.$message.success(response.msg || '签到成功');
            setTimeout(() => {
              this.$router.push({
                name: 'RegisteredActivity',
                query: { id: this.id },
              });
            }, 800);
          } else {
            this.$message.error(response.msg || '签到失败');
          }
        })
        .catch(() => {
          this.$message.error('签到请求失败，请检查网络');
        })
        .finally(() => {
          this.signingIn = false;
        });
    },
  },
};
</script>

<style scoped>
.sign-in-page {
  min-height: calc(100vh - 48px);
  padding: 12px 12px 24px;
  background: var(--vol-bg);
  box-sizing: border-box;
}

.activity-brief {
  margin-bottom: 10px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--vol-surface);
  border: 1px solid var(--vol-border);
}

.activity-brief-title {
  font-size: 16px;
  font-weight: 600;
  color: #243746;
}

.activity-brief-address {
  margin-top: 6px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.map-panel {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--vol-border);
  box-shadow: 0 5px 16px rgba(22, 119, 166, 0.1);
}

.map-container {
  width: 100%;
  height: calc(100vh - 360px);
  min-height: 240px;
  max-height: 420px;
}

.map-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.88);
  color: var(--vol-primary-strong);
  font-size: 14px;
}

.map-overlay--error {
  color: #e6a23c;
  padding: 16px;
  text-align: center;
}

.location-card {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--vol-surface);
  border: 1px solid var(--vol-border);
}

.location-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.location-row .label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.location-address {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
  word-break: break-word;
}

.location-coords {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.location-tip {
  margin-top: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f0f9ff;
  color: #1677a6;
  font-size: 12px;
  line-height: 1.5;
}

.sign-btn {
  width: 100%;
  margin-top: 16px;
  height: 44px;
  font-size: 16px;
  background: var(--vol-primary);
  border-color: var(--vol-primary);
}
</style>
