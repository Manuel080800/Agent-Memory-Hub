import { loadI18nMessages } from '../i18n.js';

const appElement = document.querySelector('#users-app');
const contextPath = appElement.dataset.contextPath;
const csrfToken = document.querySelector('meta[name="csrf-token"]').content;
const translations = await loadI18nMessages(contextPath, 'ui');
const messages = {
    requestFailed: translations['ui.request.failed'],
    requestError: translations['ui.request.error'],
    userCreated: translations['ui.user.created'],
    userUpdated: translations['ui.user.updated'],
    userDeleteConfirm: translations['ui.user.delete.confirm'],
    userDeleted: translations['ui.user.deleted'],
    passwordTooShort: translations['ui.user.password.tooShort'],
    userCreateTitle: translations['ui.user.create'],
    fullName: translations['ui.user.fullName'],
    email: translations['ui.user.email'],
    initialPassword: translations['ui.user.initialPassword'],
    role: translations['ui.role'],
    roleUser: translations['ui.role.user'],
    roleAdmin: translations['ui.role.admin'],
    enabled: translations['ui.enabled'],
    confirmationTitle: translations['ui.dialog.confirmation.title'],
    requiredFields: translations['ui.dialog.required'],
    delete: translations['ui.delete'],
    create: translations['ui.create']
};
const { createApp } = window.Vue;

createApp({
    data() {
        return {
            userList: [],
            errorMessage: '',
            noticeMessage: '',
            dialogMode: '',
            dialogTitle: '',
            dialogMessage: '',
            dialogFields: [],
            dialogValues: {},
            dialogSubmitLabel: '',
            dialogError: '',
            dialogResolve: null
        };
    },
    mounted() {
        document.getElementById('users-dialog').addEventListener('hidden.bs.modal', this.cancelDialog);
        this.loadUsers();
    },
    methods: {
        showFormDialog(title, fields) {
            return new Promise(resolve => {
                this.dialogMode = 'form';
                this.dialogTitle = title;
                this.dialogMessage = '';
                this.dialogFields = fields;
                this.dialogValues = Object.fromEntries(fields.map(field => [field.key, field.value ?? '']));
                this.dialogSubmitLabel = messages.create;
                this.dialogError = '';
                this.dialogResolve = resolve;
                this.$nextTick(() => document.getElementById('users-dialog-trigger').click());
            });
        },
        showConfirmDialog(message) {
            return new Promise(resolve => {
                this.dialogMode = 'confirm';
                this.dialogTitle = messages.confirmationTitle;
                this.dialogMessage = message;
                this.dialogFields = [];
                this.dialogValues = {};
                this.dialogSubmitLabel = messages.delete;
                this.dialogError = '';
                this.dialogResolve = resolve;
                this.$nextTick(() => document.getElementById('users-dialog-trigger').click());
            });
        },
        completeDialog(result) {
            const resolve = this.dialogResolve;
            this.dialogResolve = null;
            if (resolve) resolve(result);
        },
        cancelDialog() {
            this.completeDialog(null);
        },
        confirmDialog() {
            this.completeDialog(true);
        },
        submitDialog() {
            if (this.dialogFields.some(field => field.required && !String(this.dialogValues[field.key] || '').trim())) {
                this.dialogError = messages.requiredFields;
                return;
            }
            if (this.dialogValues.password && this.dialogValues.password.length < 12) {
                this.dialogError = messages.passwordTooShort;
                return;
            }
            this.completeDialog({ ...this.dialogValues });
            document.getElementById('users-dialog-close').click();
        },
        async request(path, options = {}) {
            const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
            if (options.method && options.method !== 'GET') headers['X-CSRF-Token'] = csrfToken;
            const response = await fetch(contextPath + path, { ...options, headers });
            if (!response.ok) {
                const result = await response.json().catch(() => ({}));
                throw new Error(result.message || result.error?.message || messages.requestFailed.replace('{0}', response.status));
            }
            return response.status === 204 ? null : response.json();
        },
        async loadUsers() {
            try {
                const page = await this.request('/users?page=0&pageSize=100');
                this.userList = page.items || [];
            } catch (error) { this.showError(error); }
        },
        async createUser() {
            const values = await this.showFormDialog(messages.userCreateTitle, [
                { key: 'fullName', label: messages.fullName, value: '', required: true },
                { key: 'email', label: messages.email, value: '', inputType: 'email', required: true },
                { key: 'password', label: messages.initialPassword, value: '', inputType: 'password', autocomplete: 'new-password', minlength: 12, required: true },
                { key: 'platformRole', label: messages.role, value: 'USER', options: [{ value: 'USER', label: messages.roleUser }, { value: 'ADMIN', label: messages.roleAdmin }] },
                { key: 'enabled', label: messages.enabled, value: true, type: 'checkbox' }
            ]);
            if (!values) return;
            try {
                await this.request('/users', { method: 'POST', body: JSON.stringify(values) });
                this.noticeMessage = messages.userCreated;
                await this.loadUsers();
            } catch (error) { this.showError(error); }
        },
        async saveUser(user) {
            if (user.passwordDraft && user.passwordDraft.length < 12) {
                this.showError(new Error(messages.passwordTooShort));
                return;
            }
            try {
                const changes = { fullName: user.fullName, email: user.email, platformRole: user.platformRole, enabled: user.enabled };
                if (user.passwordDraft) changes.password = user.passwordDraft;
                const updatedUser = await this.request('/users/' + user.id, { method: 'PUT', headers: { 'If-Match': String(user.version) }, body: JSON.stringify(changes) });
                Object.assign(user, updatedUser);
                user.passwordDraft = '';
                this.noticeMessage = messages.userUpdated;
            } catch (error) { this.showError(error); }
        },
        async deleteUser(user) {
            if (!await this.showConfirmDialog(messages.userDeleteConfirm.replace('{0}', user.fullName))) return;
            try {
                await this.request('/users/' + user.id, { method: 'DELETE' });
                this.userList = this.userList.filter(item => item.id !== user.id);
                this.noticeMessage = messages.userDeleted;
            } catch (error) { this.showError(error); }
        },
        showError(error) {
            this.errorMessage = error.message || messages.requestError;
            this.noticeMessage = '';
        }
    }
}).mount('#users-app');
