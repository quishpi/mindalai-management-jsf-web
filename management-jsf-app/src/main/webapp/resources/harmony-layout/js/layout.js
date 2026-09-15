/** 
 * PrimeFaces Harmony Layout
 */
PrimeFaces.widget.Harmony = PrimeFaces.widget.BaseWidget.extend({
    
    init: function(cfg) {
        this._super(cfg);
        this.wrapper = $(document.body).children('.layout-wrapper');
        this.topbar = this.wrapper.children('.layout-topbar');
        this.sidebar = this.wrapper.children('.layout-sidebar');
        this.menuContainer = this.wrapper.children('.layout-menu-container');
        this.menu = this.jq;
        this.menulinks = this.menu.find('a');
        this.rootMenuLinks = this.menu.find('> li > a');
        this.topbarSidebarButton = $('#topbar-sidebar-button');
        this.profileMenuButton = $('#topbar-profile-menu-button');
        this.profileMenu = $('#topbar-profile-menu');
        this.profileMenuLinks = this.profileMenu.find('a');
        this.quickMenuButton = $('#topbar-quickmenu-button');
        this.quickMenu = this.topbar.find('> .topbar-content > .topbar-icons');
        this.expandedMenuitems = this.expandedMenuitems||[];
        this.rightPanelButton = $('#topbar-right-panel-button');
        this.rightPanel = $('#layout-right-panel');

        this.configButton = $('#layout-config-button');
        this.configurator = this.wrapper.children('.layout-config');
        this.configClicked = false;

        this._bindEvents();

        var isSlimMenu = this.wrapper.hasClass('layout-menu-slim'),
        isHorizontalMenu = this.wrapper.hasClass('layout-menu-horizontal'),
        isDesktop = this.isDesktop();

        if(!(isSlimMenu && isDesktop) && !(isHorizontalMenu && isDesktop)) {
            this.restoreMenuState();
        }
    },

    _bindEvents: function() {
        var $this = this;

        this.menu.off('click.menu').on('click.menu', function() {
            $this.menuClick = true;
        });

        this.profileMenu.off('click.profile').on('click.profile', function() {
            $this.profileMenuClick = true;
        });

        this.rightPanel.off('click.rightpanel').on('click.rightpanel', function() {
            $this.rightPanelClick = true;
        });

        this.menulinks.off('click.menu').on('click.menu', function (e) {
            var link = $(this),
            item = link.parent(),
            submenu = item.children('ul');

            if (item.hasClass('active-menuitem')) {
                if (submenu.length) {
                    $this.removeMenuitem(item.attr('id'));
                    item.removeClass('active-menuitem');

                    if($this.isSlimMenu() || $this.isHorizontalMenu())
                        submenu.hide();
                    else
                        submenu.slideUp();
                }

                if(item.parent().is($this.jq)) {
                    $this.menuActive = false;
                }
            }
            else {
                $this.addMenuitem(item.attr('id'));

                if($this.isSlimMenu() || $this.isHorizontalMenu()) {
                    $this.deactivateItems(item.siblings(), false);

                    if(submenu.length === 0) {
                        $this.resetMenu();
                    }
                }
                else {
                    $this.deactivateItems(item.siblings(), true);
                    $.cookie('harmony_menu_scroll_state', link.attr('href') + ',' + $this.menuContainer.scrollTop(), { path: '/' });
                }
                
                $this.activate(item);
                
                if(item.parent().is($this.jq)) {
                    $this.menuActive = true;
                }
            }

            if (submenu.length) {
                e.preventDefault();
            }
        });

        this.rootMenuLinks.off('mouseenter.menu').on('mouseenter.menu', function (e) {
            if( ($this.isSlimMenu() || $this.isHorizontalMenu()) && $this.menuActive) {
                var link = $(this);
                var item = link.parent();
                $this.deactivateItems(item.siblings(), false);
                $this.activate(item);
            }
        });

        this.topbarSidebarButton.off('click.topbar').on('click.topbar', function(e) {
            $this.menuClick = true;

            if($this.isMobile()) {
                $this.wrapper.toggleClass('layout-menu-mobile-active');
            }
            else {
                if($this.isStaticMenu())
                    $this.wrapper.toggleClass('layout-menu-static-inactive');
                else
                    $this.wrapper.toggleClass('layout-menu-overlay-active');
            }

            setTimeout(function() {
                $(window).trigger('resize');
            }, 200);
            
            e.preventDefault();
        });

        this.profileMenuButton.off('click.profile').on('click.profile', function(e) {
            $this.profileMenuClick = true;

            if($this.profileMenu.hasClass('topbar-profile-menu-active'))
                $this.hideProfileMenu();
            else
                $this.profileMenu.addClass('topbar-profile-menu-active fadeInDown');
            
            e.preventDefault();
        });

        this.profileMenuLinks.off('click.profile').on('click.profile', function (e) {
            var link = $(this);
            var item = link.parent();
            var submenu = item.children('ul');

            if(item.hasClass('menuitem-active')) {
                item.removeClass('menuitem-active');
                submenu.slideUp();
            }
            else {
                item.siblings('.menuitem-active').removeClass('menuitem-active').children('ul').slideUp();
                item.addClass('menuitem-active');
                submenu.slideDown();
            }
            
            var href = link.attr('href');
            if(href && href !== '#') {
                window.location.href = href;
            }

            e.preventDefault();
        });

        this.quickMenuButton.off('click.menu').on('click.menu', function (e) {
            $this.wrapper.toggleClass('topbar-icons-active');
            $this.quickMenuButtonClick = true;
            e.preventDefault();
        });

        this.rightPanelButton.off('click.rightpanel').on('click.rightpanel', function (e) {
            $this.rightPanelClick = true;
            $this.rightPanel.toggleClass('layout-right-panel-active');
            e.preventDefault();
        });

        this.configButton.off('click.configbutton').on('click.configbutton', function(e) {
            $this.configurator.toggleClass('layout-config-active');
            $this.configClicked = true;
        });

        this.configurator.off('click.config').on('click.config', function() {
            $this.configClicked = true;
        });

        $(document.body).off('click.layoutBody').on('click.layoutBody', function() {
            if(($this.isHorizontalMenu() || $this.isSlimMenu()) && !$this.menuClick && $this.isDesktop()) {
                $this.resetMenu(); 
            }
            
            if(!$this.profileMenuClick && $this.profileMenu.hasClass('topbar-profile-menu-active')) {
                $this.hideProfileMenu();
            }

            if(!$this.rightPanelClick) {
                $this.rightPanel.removeClass('layout-right-panel-active');
            }

            if(!$this.quickMenuButtonClick && $this.wrapper.hasClass('topbar-icons-active') && !$this.isDatePickerPanelClicked()) {
                $this.wrapper.removeClass('topbar-icons-active');
            }
            
            if(!$this.menuClick) {
                $this.wrapper.removeClass('layout-menu-overlay-active layout-menu-mobile-active');
            }

            if (!$this.configClicked && $this.configurator.hasClass('layout-config-active')) {
                $this.configurator.removeClass('layout-config-active');
            }
            
            $this.menuClick = false;
            $this.profileMenuClick = false;
            $this.rightPanelClick = false;
            $this.quickMenuButtonClick = false;
            $this.configClicked = false;
        });
    },

    isDatePickerPanelClicked: function () {
        if ($.datepicker) {
            var input = $($.datepicker._lastInput);
            if (input.closest('.layout-rightpanel').length && $('#ui-datepicker-div').is(':visible')) {
                return true;
            }
        }
        return false;
    },

    resetMenu: function() {
        this.menu.find('.active-menuitem').removeClass('active-menuitem');
        this.menu.find('ul:visible').hide();
        this.menuActive = false;
    },

    removeMenuitem: function (id) {
        this.expandedMenuitems = $.grep(this.expandedMenuitems, function (value) {
            return value !== id;
        });
        this.saveMenuState();
    },

    addMenuitem: function (id) {
        if ($.inArray(id, this.expandedMenuitems) === -1) {
            this.expandedMenuitems.push(id);
        }
        this.saveMenuState();
    },

    hideProfileMenu: function() {
        var $this = this;
        this.profileMenu.addClass('fadeOutUp');

        setTimeout(function() {
            $this.profileMenu.removeClass('topbar-profile-menu-active fadeOutUp');
        }, 450);
    },

    saveMenuState: function() {
        $.cookie('harmony_expandeditems', this.expandedMenuitems.join(','), {path: '/'});
    },

    clearMenuState: function() {
        $.removeCookie('harmony_expandeditems', {path: '/'});
    },

    activate: function(item) {
        var submenu = item.children('ul');
        item.addClass('active-menuitem');

        if(submenu.length) {
            if(this.isSlimMenu() || this.isHorizontalMenu())
                submenu.show();
            else
                submenu.slideDown();
        }
    },

    deactivate: function(item) {
        var submenu = item.children('ul');
        item.removeClass('active-menuitem');

        if(submenu.length) {
            submenu.hide();
        }
    },

    deactivateItems: function(items, animate) {
        var $this = this;

        for(var i = 0; i < items.length; i++) {
            var item = items.eq(i),
            submenu = item.children('ul');

            if(submenu.length) {
                if(item.hasClass('active-menuitem')) {
                    var activeSubItems = item.find('.active-menuitem');
                    item.removeClass('active-menuitem');

                    if(animate) {
                        submenu.slideUp('normal', function() {
                            $(this).parent().find('.active-menuitem').each(function() {
                                $this.deactivate($(this));
                            });
                        });
                    }
                    else {
                        submenu.hide();
                        item.find('.active-menuitem').each(function() {
                            $this.deactivate($(this));
                        });
                    }

                    $this.removeMenuitem(item.attr('id'));
                    activeSubItems.each(function() {
                        $this.removeMenuitem($(this).attr('id'));
                    });
                }
                else {
                    item.find('.active-menuitem').each(function() {
                        var subItem = $(this);
                        $this.deactivate(subItem);
                        $this.removeMenuitem(subItem.attr('id'));
                    });
                }
            }
            else if(item.hasClass('active-menuitem')) {
                $this.deactivate(item);
                $this.removeMenuitem(item.attr('id'));
            }
        }
    },

    restoreMenuState: function() {
        var $this = this;
        var menucookie = $.cookie('harmony_expandeditems');
        if (menucookie) {
            this.expandedMenuitems = menucookie.split(',');
            for (var i = 0; i < this.expandedMenuitems.length; i++) {
                var id = this.expandedMenuitems[i];
                if (id) {
                    var menuitem = $("#" + this.expandedMenuitems[i].replace(/:/g, "\\:").replace(/\|/g,"\\|"));
                    menuitem.addClass('active-menuitem');

                    var submenu = menuitem.children('ul');
                    if(submenu.length) {
                        submenu.show();
                    }
                }
            }

            setTimeout(function() {
                $this.restoreScrollState(menuitem);
            }, 100)
        }
    },

    restoreScrollState: function(menuitem) {
        var scrollState = $.cookie('harmony_menu_scroll_state');
        if (scrollState) {
            var state = scrollState.split(',');
            if (state[0].startsWith(this.cfg.pathname) || this.isScrolledIntoView(menuitem, state[1])) {
                this.menuContainer.scrollTop(parseInt(state[1], 10));
            }
            else {
                this.scrollIntoView(menuitem.get(0));
                $.removeCookie('harmony_menu_scroll_state', { path: '/' });
            }
        }
        else if (!this.isScrolledIntoView(menuitem, menuitem.scrollTop())){
            this.scrollIntoView(menuitem.get(0));
        }
    },

    scrollIntoView: function(elem) {
        if (document.documentElement.scrollIntoView && elem) {
            elem.scrollIntoView({ block: "nearest", inline: 'start' });

            var container = $('.layout-menu-container');
            var scrollTop = container.scrollTop();
            if (scrollTop > 0) {
                container.scrollTop(scrollTop + parseFloat(this.topbar.height()));
            }
        }
    },

    isScrolledIntoView: function(elem, scrollTop) {
        if (elem && elem.length > 0) {
            var viewBottom = parseInt(scrollTop, 10) + this.menuContainer.height();

            var elemTop = elem.position().top;
            var elemBottom = elemTop + elem.height();

            return ((elemBottom <= viewBottom) && (elemTop >= scrollTop));
        }

        return false;
    },

    isStaticMenu: function() {
        return this.wrapper.hasClass('layout-menu-static') && this.isDesktop();
    },

    isSlimMenu: function() {
        return this.wrapper.hasClass('layout-menu-slim') && this.isDesktop();
    },

    isHorizontalMenu: function() {
        return this.wrapper.hasClass('layout-menu-horizontal') && this.isDesktop();
    },

    isMobile: function() {
        return window.innerWidth <= 1024;
    },

    isDesktop: function() {
        return window.innerWidth > 1024;
    }
});

PrimeFaces.HarmonyConfigurator = {

    changeMenuMode: function(menuMode) {
        var wrapper = $(document.body).children('.layout-wrapper');
        switch (menuMode) {
            case 'layout-menu-static':
                wrapper.addClass('layout-menu-static').removeClass('layout-menu-overlay layout-menu-slim layout-menu-horizontal');
                this.clearLayoutState();
                break;

            case 'layout-menu-overlay':
                wrapper.addClass('layout-menu-overlay').removeClass('layout-menu-static layout-menu-slim layout-menu-horizontal');
                this.clearLayoutState();
                break;

            case 'layout-menu-slim':
                wrapper.addClass('layout-menu-slim').removeClass('layout-menu-static layout-menu-overlay layout-menu-horizontal');
                this.clearLayoutState();
                break;

            case 'layout-menu-horizontal':
                wrapper.addClass('layout-menu-horizontal').removeClass('layout-menu-static layout-menu-overlay layout-menu-slim');
                this.clearLayoutState();
                break;

            default:
                wrapper.addClass('layout-menu-static').removeClass('layout-menu-overlay layout-menu-slim layout-menu-static');
                this.clearLayoutState();
                break;
        }
    },

    changeLayout: function(layoutTheme) {
        var linkElement = $('link[href*="layout-"]');
        var href = linkElement.attr('href');
        var startIndexOf = href.indexOf('layout-') + 7;
        var endIndexOf = href.indexOf('.css');
        var currentColor = href.substring(startIndexOf, endIndexOf);

        this.replaceLink(linkElement, href.replace(currentColor, layoutTheme));
    },

    changeScheme: function(theme) {
        var library = 'primefaces-harmony';
        var linkElement = $('link[href*="theme.css"]');
        var href = linkElement.attr('href');
        var index = href.indexOf(library) + 1;
        var currentTheme = href.substring(index + library.length);

        this.replaceLink(linkElement, href.replace(currentTheme, theme));
        this.changeLayout(theme);
    },

    changeMenuToRTL: function() {
        var wrapper = $('.layout-wrapper');
        wrapper.toggleClass('layout-rtl');
    },

    beforeResourceChange: function() {
        PrimeFaces.ajax.RESOURCE = null;    //prevent resource append
    },

    replaceLink: function(linkElement, href) {
        PrimeFaces.ajax.RESOURCE = 'javax.faces.Resource';

        var isIE = this.isIE();

        if (isIE) {
            linkElement.attr('href', href);
        }
        else {
            var cloneLinkElement = linkElement.clone(false);

            cloneLinkElement.attr('href', href);
            linkElement.after(cloneLinkElement);

            cloneLinkElement.off('load').on('load', function() {
                linkElement.remove();
            });
        }
    },

    clearLayoutState: function() {
        var menu = PF('HarmonyMenuWidget');

        if (menu) {
            menu.clearMenuState();
        }
    },

    isIE: function() {
        return /(MSIE|Trident\/|Edge\/)/i.test(navigator.userAgent);
    },

    updateInputStyle: function(value) {
        if (value === 'filled')
            $(document.body).addClass('ui-input-filled');
        else
            $(document.body).removeClass('ui-input-filled');
    }
};

/*!
 * jQuery Cookie Plugin v1.4.1
 * https://github.com/carhartl/jquery-cookie
 *
 * Copyright 2006, 2014 Klaus Hartl
 * Released under the MIT license
 */
(function (factory) {
    if (typeof define === 'function' && define.amd) {
        // AMD (Register as an anonymous module)
        define(['jquery'], factory);
    } else if (typeof exports === 'object') {
        // Node/CommonJS
        module.exports = factory(require('jquery'));
    } else {
        // Browser globals
        factory(jQuery);
    }
}(function ($) {

    var pluses = /\+/g;

    function encode(s) {
        return config.raw ? s : encodeURIComponent(s);
    }

    function decode(s) {
        return config.raw ? s : decodeURIComponent(s);
    }

    function stringifyCookieValue(value) {
        return encode(config.json ? JSON.stringify(value) : String(value));
    }

    function parseCookieValue(s) {
        if (s.indexOf('"') === 0) {
            // This is a quoted cookie as according to RFC2068, unescape...
            s = s.slice(1, -1).replace(/\\"/g, '"').replace(/\\\\/g, '\\');
        }

        try {
            // Replace server-side written pluses with spaces.
            // If we can't decode the cookie, ignore it, it's unusable.
            // If we can't parse the cookie, ignore it, it's unusable.
            s = decodeURIComponent(s.replace(pluses, ' '));
            return config.json ? JSON.parse(s) : s;
        } catch (e) { }
    }

    function read(s, converter) {
        var value = config.raw ? s : parseCookieValue(s);
        return $.isFunction(converter) ? converter(value) : value;
    }

    var config = $.cookie = function (key, value, options) {

        // Write

        if (arguments.length > 1 && !$.isFunction(value)) {
            options = $.extend({}, config.defaults, options);

            if (typeof options.expires === 'number') {
                var days = options.expires, t = options.expires = new Date();
                t.setMilliseconds(t.getMilliseconds() + days * 864e+5);
            }

            return (document.cookie = [
                encode(key), '=', stringifyCookieValue(value),
                options.expires ? '; expires=' + options.expires.toUTCString() : '', // use expires attribute, max-age is not supported by IE
                options.path ? '; path=' + options.path : '',
                options.domain ? '; domain=' + options.domain : '',
                options.secure ? '; secure' : ''
            ].join(''));
        }

        // Read

        var result = key ? undefined : {},
            // To prevent the for loop in the first place assign an empty array
            // in case there are no cookies at all. Also prevents odd result when
            // calling $.cookie().
            cookies = document.cookie ? document.cookie.split('; ') : [],
            i = 0,
            l = cookies.length;

        for (; i < l; i++) {
            var parts = cookies[i].split('='),
                name = decode(parts.shift()),
                cookie = parts.join('=');

            if (key === name) {
                // If second argument (value) is a function it's a converter...
                result = read(cookie, value);
                break;
            }

            // Prevent storing a cookie that we couldn't decode.
            if (!key && (cookie = read(cookie)) !== undefined) {
                result[name] = cookie;
            }
        }

        return result;
    };

    config.defaults = {};

    $.removeCookie = function (key, options) {
        // Must not alter options, thus extending a fresh object...
        $.cookie(key, '', $.extend({}, options, { expires: -1 }));
        return !$.cookie(key);
    };

}));

/* Issue #924 is fixed for 5.3+ and 6.0. (compatibility with 5.3) */
if(window['PrimeFaces'] && window['PrimeFaces'].widget.Dialog) {
    PrimeFaces.widget.Dialog = PrimeFaces.widget.Dialog.extend({
        
        enableModality: function() {
            this._super();
            $(document.body).children(this.jqId + '_modal').addClass('ui-dialog-mask');
        },
        
        syncWindowResize: function() {}
    });
}

/* Issue #2131 */
if(window['PrimeFaces'] && window['PrimeFaces'].widget.Schedule && isLtPF8Version()) {
    PrimeFaces.widget.Schedule = PrimeFaces.widget.Schedule.extend({
        
        setupEventSource: function() {
            var $this = this,
            offset = moment().utcOffset()*60000;

            this.cfg.events = function(start, end, timezone, callback) {
                var options = {
                    source: $this.id,
                    process: $this.id,
                    update: $this.id,
                    formId: $this.cfg.formId,
                    params: [
                        {name: $this.id + '_start', value: start.valueOf() + offset},
                        {name: $this.id + '_end', value: end.valueOf() + offset}
                    ],
                    onsuccess: function(responseXML, status, xhr) {
                        PrimeFaces.ajax.Response.handle(responseXML, status, xhr, {
                                widget: $this,
                                handle: function(content) {
                                    callback($.parseJSON(content).events);
                                }
                            });

                        return true;
                    }
                };

                PrimeFaces.ajax.Request.handle(options);
            }
        }
    });
}

function isLtPF8Version() {
    var version = window['PrimeFaces'].VERSION;
    if (!version) {
        return true;
    }

    return parseInt(version.split('.')[0], 10) < 8;
}

if (PrimeFaces.widget.SelectOneMenu) {
    PrimeFaces.widget.SelectOneMenu = PrimeFaces.widget.SelectOneMenu.extend({
        init: function (cfg) {
            this._super(cfg);

            var $this = this;
            if (this.jq.parent().hasClass('ui-float-label')) {
                this.m_panel = $(this.jqId + '_panel');
                this.m_focusInput = $(this.jqId + '_focus');

                this.m_panel.addClass('ui-input-overlay-panel');
                this.jq.addClass('ui-inputwrapper');

                if (this.input.val() != '') {
                    this.jq.addClass('ui-inputwrapper-filled');
                }

                this.input.off('change').on('change', function () {
                    $this.inputValueControl($(this));
                });

                this.m_focusInput.on('focus.ui-selectonemenu', function () {
                    $this.jq.addClass('ui-inputwrapper-focus');
                })
                    .on('blur.ui-selectonemenu', function () {
                        $this.jq.removeClass('ui-inputwrapper-focus');
                    });

                if (this.cfg.editable) {
                    this.label.on('input', function (e) {
                        $this.inputValueControl($(this));
                    }).on('focus', function () {
                        $this.jq.addClass('ui-inputwrapper-focus');
                    }).on('blur', function () {
                        $this.jq.removeClass('ui-inputwrapper-focus');
                        $this.inputValueControl($(this));
                    });
                }
            }
        },

        inputValueControl: function (input) {
            if (input.val() != '')
                this.jq.addClass('ui-inputwrapper-filled');
            else
                this.jq.removeClass('ui-inputwrapper-filled');
        }
    });
}

if (PrimeFaces.widget.Chips) {
    PrimeFaces.widget.Chips = PrimeFaces.widget.Chips.extend({
        init: function (cfg) {
            this._super(cfg);

            var $this = this;
            if (this.jq.parent().hasClass('ui-float-label')) {
                this.jq.addClass('ui-inputwrapper');

                if ($this.jq.find('.ui-chips-token').length !== 0) {
                    this.jq.addClass('ui-inputwrapper-filled');
                }

                this.input.on('focus.ui-chips', function () {
                    $this.jq.addClass('ui-inputwrapper-focus');
                }).on('input.ui-chips', function () {
                    $this.inputValueControl();
                }).on('blur.ui-chips', function () {
                    $this.jq.removeClass('ui-inputwrapper-focus');
                    $this.inputValueControl();
                });

            }
        },

        inputValueControl: function () {
            if (this.jq.find('.ui-chips-token').length !== 0 || this.input.val() != '')
                this.jq.addClass('ui-inputwrapper-filled');
            else
                this.jq.removeClass('ui-inputwrapper-filled');
        }
    });
}

if (PrimeFaces.widget.DatePicker) {
    PrimeFaces.widget.DatePicker = PrimeFaces.widget.DatePicker.extend({
        init: function (cfg) {
            this._super(cfg);

            var $this = this;
            if (this.jq.parent().hasClass('ui-float-label') && !this.cfg.inline) {
                if (this.input.val() != '') {
                    this.jq.addClass('ui-inputwrapper-filled');
                }

                this.jqEl.off('focus.ui-datepicker blur.ui-datepicker change.ui-datepicker')
                    .on('focus.ui-datepicker', function () {
                        $this.jq.addClass('ui-inputwrapper-focus');
                    })
                    .on('blur.ui-datepicker', function () {
                        $this.jq.removeClass('ui-inputwrapper-focus');
                    })
                    .on('change.ui-datepicker', function () {
                        $this.inputValueControl($(this));
                    });
            }
        },

        inputValueControl: function (input) {
            if (input.val() != '')
                this.jq.addClass('ui-inputwrapper-filled');
            else
                this.jq.removeClass('ui-inputwrapper-filled');
        }
    });
}