/**
 * Cobbled together from lots of different switch style plugins. None of them worked how I liked.
 */

(function($){
    var defaultOptions = {
        labelClass: null,
        width: "63px",
        height: "18px",
        onText: "On",
        offText: "Off",
        backgroundImage: "images/on-off-background.png",
        foregroundImage: "images/on-off-foreground.png",
        verticalAlign: 'text-top',
        cursor: "pointer",
        callback: null
    };

    function toggle(jqueryElement, doCallback) {
        var data = jqueryElement.data("onOff");
        var ourSpan = jqueryElement.next().next();
        if ( jqueryElement.prop("checked") )
        {
            ourSpan.css('backgroundPosition', 'top right');
            jqueryElement.prop("checked", false);
        }
        else
        {
            ourSpan.css('backgroundPosition', 'top left');
            jqueryElement.prop("checked", true);
        }

        if ( doCallback && data.options.callback )
        {
            data.options.callback(jqueryElement.prop("checked"));
        }
    }

    function initialize(domElement, jqueryElement, options) {
        if ( jqueryElement.data("onOff") ) {
            return; // already initialized
        }

        options = $.extend({}, defaultOptions, options);

        var     data = {};
        data.id = domElement.id;
        data.options = options;
        jqueryElement.data("onOff", data);

        var onOffHtml = '';
        onOffHtml += "<span";
        if ( options.labelClass )
        {
            onOffHtml += ' class="' + options.labelClass + '"';
        }
        onOffHtml += ">";
        onOffHtml += options.offText;
        onOffHtml += '</span> ';
        onOffHtml += ' ';
        onOffHtml += '<span style="';
        onOffHtml += 'display: inline-block;';
        onOffHtml += 'background: transparent url(\'' + options.backgroundImage + '\') no-repeat top left;';
        onOffHtml += 'width: ' + options.width + ';';
        onOffHtml += 'height: ' + options.height + ';';
        onOffHtml += 'vertical-align: ' + options.verticalAlign + ';';
        onOffHtml += 'cursor: ' + options.cursor + ';';
        onOffHtml += '">';
        onOffHtml += '<img src="' + options.foregroundImage + '" width="' + options.width + '" height="' + options.height + '" border="0" alt="">';
        onOffHtml += '</span>';
        onOffHtml += ' ';
        onOffHtml += "<span";
        if ( options.labelClass )
        {
            onOffHtml += ' class="' + options.labelClass + '"';
        }
        onOffHtml += ">";
        onOffHtml += options.onText;
        onOffHtml += '</span> ';

        jqueryElement.after(onOffHtml);
        jqueryElement.hide();

        if ( !jqueryElement.prop("checked") )
        {
            toggle(jqueryElement, false);
        }

        jqueryElement.next().next().click(function(){
            if ( !jqueryElement.prop("disabled") )
            {
                toggle(jqueryElement, true);
            }
        });
    }

    var methods = {
        initialize: initialize,

        setChecked: function(domElement, jqueryElement, check) {
            if ( jqueryElement.prop("checked") != check )
            {
                toggle(jqueryElement, false);
            }
        },

        setCallback: function(domElement, jqueryElement, callback) {
            var data = jqueryElement.data("onOff");
            data.options.callback = callback;
        },

        setEnabled: function(domElement, jqueryElement, enabled) {
            if ( jqueryElement.prop("disabled") != !enabled )
            {
                var ourSpan = jqueryElement.next().next();
                var data = jqueryElement.data("onOff");

                jqueryElement.prop("disabled", !enabled);
                ourSpan.css("opacity", enabled ? "1.0" : ".4");
                ourSpan.css("cursor", enabled ? data.options.cursor : "default");
            }
        }
    };

    $.fn.onOff = function(method, argument){
        return this.each(function() {
            var jqueryElement = $(this);

            if ( methods[method] )
            {
                return methods[method].call(this, this, jqueryElement, argument);
            }
            else if ( typeof (method === 'object') || !method )
            {
                return methods.initialize(this, jqueryElement, method); // method is argument here
            }
            else
            {
                $.error('Method ' +  method + ' does not exist on jQuery.onOff');
            }
        });
    };
})(jQuery);
