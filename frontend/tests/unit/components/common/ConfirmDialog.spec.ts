import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';

describe('ConfirmDialog', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  afterEach(() => {
    document.body.innerHTML = '';
  });

  it('RED: props.visible=true 时渲染标题、描述、确认/取消按钮', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        visible: true,
        title: '确认删除',
        description: '确定要删除吗？',
        confirmText: '确认',
        cancelText: '取消',
      },
    });

    expect(wrapper.text()).toContain('确认删除');
    expect(wrapper.text()).toContain('确定要删除吗？');
    expect(wrapper.text()).toContain('确认');
    expect(wrapper.text()).toContain('取消');
  });

  it('RED: 点击"确认"按钮触发 confirm 事件', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: { visible: true, title: 'Test' },
    });

    await wrapper.find('.btn-confirm').trigger('click');

    expect(wrapper.emitted('confirm')).toBeTruthy();
  });

  it('RED: 点击"取消"按钮触发 cancel 事件', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: { visible: true, title: 'Test' },
    });

    await wrapper.find('.btn-cancel').trigger('click');

    expect(wrapper.emitted('cancel')).toBeTruthy();
  });

  it('RED: visible=false 时不渲染 DOM', () => {
    const wrapper = mount(ConfirmDialog, {
      props: { visible: false, title: 'Test' },
    });

    expect(wrapper.find('.dialog-overlay').exists()).toBe(false);
  });

  it('RED: 按 Esc 触发 cancel 事件', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: { visible: true, title: 'Test' },
    });

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(wrapper.emitted('cancel')).toBeTruthy();
    expect(wrapper.emitted('update:visible')).toBeTruthy();
    expect(wrapper.emitted('update:visible')![0]).toEqual([false]);
  });

  it('RED: 点击遮罩触发 cancel 事件', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: { visible: true, title: 'Test' },
      attachTo: document.body,
    });

    await wrapper.find('.dialog-overlay').trigger('click');

    expect(wrapper.emitted('cancel')).toBeTruthy();
  });

  it('RED: danger=true 时"确认"按钮带 btn-danger class', () => {
    const wrapper = mount(ConfirmDialog, {
      props: { visible: true, title: 'Test', danger: true },
    });

    expect(wrapper.find('.btn-confirm.btn-danger').exists()).toBe(true);
  });

  it('RED: danger=false 时"确认"按钮没有 btn-danger class', () => {
    const wrapper = mount(ConfirmDialog, {
      props: { visible: true, title: 'Test', danger: false },
    });

    expect(wrapper.find('.btn-danger').exists()).toBe(false);
  });

  it('RED: role="dialog"、aria-modal="true"、aria-labelledby、aria-describedby 属性正确', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        visible: true,
        title: '确认删除',
        description: '确定要删除吗？',
      },
    });

    const dialog = wrapper.find('[role="dialog"]');
    expect(dialog.exists()).toBe(true);
    expect(dialog.attributes('aria-modal')).toBe('true');
    expect(dialog.attributes('aria-labelledby')).toBe('confirm-dialog-title');
    expect(dialog.attributes('aria-describedby')).toBe('confirm-dialog-desc');
  });

  it('RED: 无 description 时不设置 aria-describedby', () => {
    const wrapper = mount(ConfirmDialog, {
      props: { visible: true, title: 'Test' },
    });

    const dialog = wrapper.find('[role="dialog"]');
    expect(dialog.attributes('aria-describedby')).toBeUndefined();
  });

  it('RED: visible 从 false 变 true 时"确认"按钮获得焦点', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: { visible: false, title: 'Test' },
      attachTo: document.body,
    });

    await wrapper.setProps({ visible: true });
    await new Promise(r => setTimeout(r, 50));

    const confirmBtn = wrapper.find('.btn-confirm').element;
    expect(document.activeElement).toBe(confirmBtn);
  });

  it('REFACTOR: loading 时确认按钮 disabled，显示 spinner 和"删除中..."', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        visible: true,
        title: 'Test',
        danger: true,
        loading: true,
      },
    });

    expect(wrapper.find('.btn-confirm').attributes('disabled')).toBeDefined();
    expect(wrapper.find('.spinner').exists()).toBe(true);
    expect(wrapper.text()).toContain('删除中...');
  });

  it('REFACTOR: loading 时取消按钮 disabled', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        visible: true,
        title: 'Test',
        loading: true,
      },
    });

    expect(wrapper.find('.btn-cancel').attributes('disabled')).toBeDefined();
  });

  it('REFACTOR: 遮罩点击只触发 cancel（不影响内部内容点击）', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: { visible: true, title: 'Test' },
      attachTo: document.body,
    });

    // Click inside dialog content — should NOT close
    await wrapper.find('.dialog-content').trigger('click');
    expect(wrapper.emitted('cancel')).toBeFalsy();

    // Click overlay — should close
    await wrapper.find('.dialog-overlay').trigger('click');
    expect(wrapper.emitted('cancel')).toBeTruthy();
  });
});
