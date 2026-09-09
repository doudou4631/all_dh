import { defineStore } from 'pinia';

const useAppStore = defineStore(
  'app',
  {
    state: () => ({
      sidebar: {
        // Keep the desktop navigation expanded. Mobile still uses the drawer behavior.
        opened: true,
        withoutAnimation: false,
        hide: false
      },
      device: 'desktop',
      size: (localStorage.getItem('size') || 'default') as 'large' | 'default' | 'small'
    }),
    actions: {
      toggleSideBar(withoutAnimation: boolean) {
        if (this.sidebar.hide) {
          return false;
        }
        if (this.device !== 'mobile') {
          this.sidebar.opened = true
          return false
        }
        this.sidebar.opened = !this.sidebar.opened
        this.sidebar.withoutAnimation = withoutAnimation
        if (this.sidebar.opened) {
          localStorage.setItem('sidebarStatus', '1')
        } else {
          localStorage.setItem('sidebarStatus', '0')
        }
      },
      closeSideBar(withoutAnimation: boolean) {
        if (this.device !== 'mobile') {
          this.sidebar.opened = true
          return false
        }
        localStorage.setItem('sidebarStatus', '0')
        this.sidebar.opened = false
        this.sidebar.withoutAnimation = withoutAnimation
      },
      toggleDevice(device: string) {
        this.device = device
      },
      setSize(size: 'large' | 'default' | 'small') {
        this.size = size;
        localStorage.setItem('size', size)
      },
      toggleSideBarHide(status: boolean) {
        this.sidebar.hide = status
      }
    }
  })

export default useAppStore
