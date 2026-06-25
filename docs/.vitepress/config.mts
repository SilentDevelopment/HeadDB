import { defineConfig } from 'vitepress';

export default defineConfig({
  title: 'HeadDB',
  description: 'Modern head database plugin for Paper and Folia servers.',
  base: '/',
  cleanUrls: true,
  lastUpdated: true,
  head: [
    ['meta', { name: 'theme-color', content: '#dc2626' }],
    ['meta', { property: 'og:title', content: 'HeadDB Documentation' }],
    ['meta', { property: 'og:description', content: 'Documentation for installing, configuring, and using HeadDB.' }]
  ],
  themeConfig: {
    logo: '/logo.svg',
    siteTitle: 'HeadDB',
    search: {
      provider: 'local'
    },
    nav: [
      { text: 'Guide', link: '/getting-started' },
      { text: 'Commands', link: '/commands' },
      { text: 'Config', link: '/configuration' },
      { text: 'API', link: '/api' }
    ],
    sidebar: [
      {
        text: 'Start',
        items: [
          { text: 'Overview', link: '/' },
          { text: 'Installation', link: '/installation' },
          { text: 'Getting Started', link: '/getting-started' }
        ]
      },
      {
        text: 'Server Owners',
        items: [
          { text: 'Commands', link: '/commands' },
          { text: 'Permissions', link: '/permissions' },
          { text: 'Configuration', link: '/configuration' },
          { text: 'GUI', link: '/gui' },
          { text: 'Heads and Local Data', link: '/heads-and-local-data' },
          { text: 'Economy', link: '/economy' },
          { text: 'Updating', link: '/updating' }
        ]
      },
      {
        text: 'Developers and Support',
        items: [
          { text: 'API', link: '/api' },
          { text: 'Troubleshooting', link: '/troubleshooting' },
          { text: 'FAQ', link: '/faq' }
        ]
      }
    ],
    socialLinks: [
      { icon: 'github', link: 'https://github.com/SilentDevelopment/HeadDB' }
    ],
    footer: {
      message: 'Released by SilentDevelopment.',
      copyright: 'HeadDB documentation covers public plugin usage only.'
    }
  }
});
