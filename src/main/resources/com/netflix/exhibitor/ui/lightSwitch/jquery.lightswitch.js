/*
 * jQuery LightSwitch plugin
 * @author admin@catchmyfame.com - http://www.catchmyfame.com
 * @version 1.0.1
 * @date December 16, 2010
 * @category jQuery plugin
 * @copyright (c) 2010 admin@catchmyfame.com (www.catchmyfame.com)
 * @license CC Attribution-Share Alike 3.0 - http://creativecommons.org/licenses/by-sa/3.0/
 */
(function ($) {
    $.fn.extend({
        lightSwitch: function (options) {
            var defaults = {
                animSpeed: 120,
                hoverSpeed: 100,
                switchImg: 'switch.png',
                switchImgCover: 'switchplate.png',
                switchImgCoverWidth: '63px',
                switchImgCoverHeight: '18px',
                disabledImg: 'disabled.png',
                onShift: '0px 0px',
                offShift: '-37px 0px',
                peekOff: '-6px 0px',
                peekOn: '-31px 0px'
            };
            var options = $.extend(defaults, options);

            return this.each(function () {
                var o = options;
                var obj = $(this);

                if ($(this).attr('disabled')) {
                    $(this).css({
                        'display': 'none'
                    }).after('<span><img src="' + o.disabledImg + '" /></span>');
                } else {
                    $(this).css({
                        'display': 'none'
                    }).after('<span class="switch"><img src="' + o.switchImgCover + '" width="' + o.switchImgCoverWidth + '" height="' + o.switchImgCoverHeight + '" /></span>'); //'display':'none'
                }
                $(this).next('span.switch').css({
                    'display': 'inline-block',
                    'background-image': 'url("' + o.switchImg + '")',
                    'background-repeat': 'no-repeat',
                    'overflow': 'hidden',
                    'cursor': 'pointer',
                    'margin-right': '2px'
                }).attr('offShift', o.offShift).attr('animSpeed', o.animSpeed);

                $(this).next('span.switch').click(function () {
                    // When we click any span image for a radio button, animate the previously selected radio button to 'off'.
                    if ($(this).prev().is(':radio')) {
                        radioGroupName = $(this).prev().attr('name');
                        var prevSel = 'input[name="' + radioGroupName + '"]' + ':checked + span';
                        //var animSpeed = ' != $(prevSel).attr('animSpeed') ? parseInt($(prevSel).attr('animSpeed'), 10) : o.animSpeed;
                        $(prevSel).css({
                            'background-position': $(prevSel).attr('offShift')
                        }); //.stop().animate({ 'background-position': $(prevSel).attr('offShift') }, animSpeed);
                    }

                    $(this).prev().click();

                    if ($(this).prev().is(':checked')) {
                        $(this).css({
                            'background-position': o.onShift
                        }); //.stop().animate({ 'background-position': o.onShift }, o.animSpeed);
                    } else {
                        $(this).css({
                            'background-position': o.offShift
                        }); //.stop().animate({ 'background-position': o.offShift }, o.animSpeed);
                    }
                }).hover(function () {
                        $(this).css({
                            'background-position': $(this).prev().is(':checked') ? o.peekOff : o.peekOn
                        }); //.stop().animate({ 'background-position': $(this).prev().is(':checked') ? o.peekOff : o.peekOn }, o.hoverSpeed);
                    }, function () {
                        $(this).css({
                            'background-position': $(this).prev().is(':checked') ? o.onShift : o.offShift
                        }); //.stop().animate({ 'background-position': $(this).prev().is(':checked') ? o.onShift : o.offShift }, o.hoverSpeed);
                    });

                $(this).next('span.switch').css({
                    'background-position': $(this).is(':checked') ? o.onShift : o.offShift
                });

                $('input + span').live('click', function () {
                    return false;
                });

                $(this).change(function () {
                    radioGroupName = $(this).attr('name');
                    if ($(this).is(':radio')) {
                        $(this).css({
                            'background-position': o.onShift
                        }); //.stop().animate({ 'background-position': o.onShift }, o.animSpeed);
                        $('input[name="' + radioGroupName + '"]' + ' + span').css({
                            'background-position': o.offShift
                        }); //.stop().animate({ 'background-position': o.offShift }, o.animSpeed);
                    }
                    $(this).next('span').css({
                        'background-position': $(this).is(':checked') ? o.onShift : o.offShift
                    }); //.stop().animate({ 'background-position': $(this).is(':checked') ? o.onShift : o.offShift }, o.animSpeed);
                });
            });
        }
    });
})(jQuery);