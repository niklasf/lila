function challengeStart(opts) {
  var selector = '.challenge-page';
  var accepting;

  lichess.socket = new lichess.StrongSocket(
    opts.socketUrl,
    opts.data.socketVersion, {
      events: {
        reload: function() {
          $.ajax({
            url: opts.xhrUrl,
            success: function(html) {
              $(selector).replaceWith($(html).find(selector));
              init();
            }
          });
        }
      }
    });

  function init() {
    if (!accepting) $('#challenge-redirect').each(function() {
      location.href = $(this).attr('href');
    });
    $(selector).find('form.accept').submit(function() {
      accepting = true;
      $(this).html('<span class="ddloader"></span>');
    });
    $(selector).find('form.xhr').submit(function(e) {
      e.preventDefault();
      $.ajax(lichess.formAjax($(this)));
      $(this).html('<span class="ddloader"></span>');
    });
    $(selector).find('input.friend-autocomplete').each(function() {
      var $input = $(this);
      lichess.userAutocomplete($input, {
        focus: 1,
        friend: 1,
        tag: 'span',
        onSelect() {
          $input.parents('form').submit();
        }
      });
    });
  }

  init();

  function pingNow() {
    if (document.getElementById('ping-challenge')) {
      try {
        lichess.socket.send('ping');
      } catch(e) {}
      setTimeout(pingNow, 2000);
    }
  }

  pingNow();
}
