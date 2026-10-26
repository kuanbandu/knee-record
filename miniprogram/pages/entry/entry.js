const store = require('../../utils/store.js');

let uid = 0;
function nextUid() {
  uid += 1;
  return `row_${Date.now()}_${uid}`;
}

Page({
  data: {
    exercises: [{ uid: nextUid(), name: '', sets: '' }],
    feelingScore: 0,
    durationMin: '',
    note: '',
  },

  onNameInput(e) {
    const index = e.currentTarget.dataset.index;
    const value = e.detail.value;
    const exercises = this.data.exercises.map((row, i) => (i === index ? { ...row, name: value } : row));
    this.setData({ exercises });
  },

  onSetsInput(e) {
    const index = e.currentTarget.dataset.index;
    const value = e.detail.value;
    const exercises = this.data.exercises.map((row, i) => (i === index ? { ...row, sets: value } : row));
    this.setData({ exercises });
  },

  onAddExercise() {
    const exercises = this.data.exercises.concat([{ uid: nextUid(), name: '', sets: '' }]);
    this.setData({ exercises });
  },

  onRemoveExercise(e) {
    const index = e.currentTarget.dataset.index;
    const exercises = this.data.exercises.filter((row, i) => i !== index);
    this.setData({ exercises });
  },

  onFeelingChange(e) {
    this.setData({ feelingScore: e.detail.value });
  },

  onDurationInput(e) {
    this.setData({ durationMin: e.detail.value });
  },

  onNoteInput(e) {
    this.setData({ note: e.detail.value });
  },

  onSave() {
    const exercises = this.data.exercises
      .filter((row) => row.name.trim() !== '')
      .map((row) => ({ name: row.name.trim(), sets: row.sets.trim() }));

    if (exercises.length === 0) {
      wx.showToast({ title: '请至少填写 1 个动作名称', icon: 'none' });
      return;
    }

    let durationMin = 0;
    if (this.data.durationMin !== '') {
      durationMin = Number(this.data.durationMin);
      if (!Number.isFinite(durationMin) || durationMin < 0 || durationMin > 600) {
        wx.showToast({ title: '锻炼时长请填 0-600 之间的分钟数', icon: 'none' });
        return;
      }
    }

    store.addRecord({
      id: `rec_${Date.now()}`,
      date: store.todayKey(),
      timestamp: Date.now(),
      feelingScore: Number(this.data.feelingScore),
      durationMin,
      note: this.data.note.trim(),
      exercises,
    });

    wx.showToast({ title: '已记录', icon: 'success' });
    setTimeout(() => wx.navigateBack(), 500);
  },
});
