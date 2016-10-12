lichess.ratingDistributionChart = function(data) {
  var total = data.freq.reduce(function (a, b) {
    return a + b
  }, 0);
  var cumul = [];
  for (var i = 0, sum = 0; i < data.freq.length; i++) {
    sum += data.freq[i];
    cumul.push(sum / total * 100);
  }

  var spinner = document.querySelector('#rating_distribution_chart .spinner');
  var canvas = document.createElement('canvas');
  spinner.parentNode.replaceChild(canvas, spinner);
  var ctx = canvas.getContext('2d');

  var gradient = ctx.createLinearGradient(0, 0, 0, 400);
  gradient.addColorStop(0, 'rgba(119, 152, 191, 0.8)');
  gradient.addColorStop(1, 'rgba(255, 255, 255, 0.8)');

  $.ajax({
    dataType: 'script',
    cache: true,
    url: 'https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.3.0/Chart.js'
  }).done(function () {
    new Chart(ctx, {
      type: 'line',
      scaleSteps: 100,
      data: {
        datasets: [{
          label: 'Cumulative',
          yAxisID: 'yCumul',
          data: cumul.map(function (y, x) {
            return {x: 800 + x * 25, y: y};
          }),
          fill: false,
          borderColor: '#dddf0d',
        }, {
          label: 'Frequency',
          yAxisID: 'yFreq',
          data: data.freq.map(function (y, x) {
            return {x: 800 + x * 25, y: y};
          }),
          backgroundColor: gradient,
          borderColor: '#7798bf'
        }]
      },
      options: {
        legend: {
          display: false
        },
        tooltips: {
          callbacks: {
            label: function(item) {
              if (item.datasetIndex === 0) return 'Cumulative: ' + item.yLabel + '%';
              else return 'Frequency: ' + item.yLabel + ' players';
            }
          }
        },
        scales: {
          xAxes: [{
            type: 'linear',
            position: 'bottom',
            ticks: {
              stepSize: 100,
              minRotation: 45
            },
            scaleLabel: {
              display: true,
              labelString: 'Glicko-2 Rating'
            }
          }],
          yAxes: [{
            type: 'linear',
            position: 'left',
            id: 'yFreq',
            ticks: {
              maxTicksLimit: 5
            },
            scaleLabel: {
              display: true,
              labelString: 'Players'
            }
          }, {
            type: 'linear',
            position: 'right',
            id: 'yCumul',
            ticks: {
              maxTicksLimit: 5,
              stepSize: 25,
              callback: function(value) {
                return value + '%';
              }
            },
            scaleLabel: {
              display: true,
              labelString: 'Cumulative'
            }
          }]
        },
        elements: {
          point: {
            radius: 0,
            hitRadius: 10,
            hoverRadius: 4
          }
        }
      }
    });
  });
};
