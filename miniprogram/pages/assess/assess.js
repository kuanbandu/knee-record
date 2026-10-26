const store = require('../../utils/store.js');

// 阶段仅作为个人资料保存，不参与任何推荐
const STAGE_OPTIONS = [
  { value: 'EARLY', label: '起步期', desc: '刚开始，动作强度最低' },
  { value: 'MID', label: '进阶期', desc: '已经练了一段时间' },
  { value: 'LATE', label: '强化期', desc: '想加大强度' },
  { value: 'PREHAB', label: '日常保养', desc: '让膝盖更稳一些' },
];

Page({
  data: {
    stageOptions: STAGE_OPTIONS,
    age: '',
    stage: '',
    bendAngle: 90,
    straightenOk: false,
    savedAtText: '',
  },

  onShow() {
    const saved = store.getAssessment();
    if (saved) {
      this.setData({
        age: saved.age == null ? '' : String(saved.age),
        stage: saved.stage,
        bendAngle: saved.bendAngle,
        straightenOk: !!saved.straightenOk,
        savedAtText: store.formatDateTime(saved.updatedAt),
      });
    }
  },

  onAgeInput(e) {
    this.setData({ age: e.detail.value });
  },

  onSelectStage(e) {
    this.setData({ stage: e.currentTarget.dataset.value });
  },

  onBendAngleChange(e) {
    this.setData({ bendAngle: e.detail.value });
  },

  onStraightenChange(e) {
    this.setData({ straightenOk: e.detail.value });
  },

  onSave() {
    const age = Number(this.data.age);
    if (!Number.isInteger(age) || age < 1 || age > 120) {
      wx.showToast({ title: '请填写 1-120 之间的年龄', icon: 'none' });
      return;
    }
    if (!this.data.stage) {
      wx.showToast({ title: '请选择当前阶段', icon: 'none' });
      return;
    }

    store.saveAssessment({
      age,
      stage: this.data.stage,
      bendAngle: Number(this.data.bendAngle),
      straightenOk: !!this.data.straightenOk,
    });
    wx.showToast({ title: '已保存', icon: 'success' });
  },
});
