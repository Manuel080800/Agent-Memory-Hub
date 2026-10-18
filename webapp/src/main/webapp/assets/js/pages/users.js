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
    userDeleted: translations['ui.user.deleted']
};
const { createApp } = window.Vue;

createApp({
    data() {
        return {
            userList: [],
            createForm: { fullName: '', email: '', password: '', platformRole: 'USER' },
            errorMessage: '',
            noticeMessage: ''
        };
    },
    mounted() {
        this.loadUsers();
    },
    methods: {
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
            try {
                await this.request('/users', { method: 'POST', body: JSON.stringify({ ...this.createForm, enabled: true }) });
                this.createForm = { fullName: '', email: '', password: '', platformRole: 'USER' };
                this.noticeMessage = messages.userCreated;
                await this.loadUsers();
            } catch (error) { this.showError(error); }
        },
        async saveUser(user) {
            try {
                const changes = { fullName: user.fullName, email: user.email, platformRole: user.platformRole, enabled: user.enabled };
                await this.request('/users/' + user.id, { method: 'PUT', body: JSON.stringify(changes) });
                this.noticeMessage = messages.userUpdated;
            } catch (error) { this.showError(error); }
        },
        async deleteUser(user) {
            if (!window.confirm(messages.userDeleteConfirm.replace('{0}', user.fullName))) return;
            try {
                await this.request('/users/' + user.id + '?confirm=true', { method: 'DELETE' });
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
