var map = L.map('map').setView([39.9042, 116.4074], 13);

L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap',
    maxZoom: 19
}).addTo(map);

var mockMarker = null;
var selectedMarker = null;
var routeLine = null;
var routeMode = false;
var routeStart = null;

function setInfoText(text) {
    var el = document.getElementById('infoBox');
    if (el) el.innerText = text;
}

map.on('click', function(e) {
    var lat = e.latlng.lat;
    var lng = e.latlng.lng;

    if (routeMode) {
        if (!routeStart) {
            routeStart = L.latLng(lat, lng);
            if (selectedMarker) map.removeLayer(selectedMarker);
            selectedMarker = L.marker([lat, lng]).addTo(map)
                .bindPopup('起点: ' + lat.toFixed(6) + ', ' + lng.toFixed(6)).openPopup();
            setInfoText('已设置起点，请点击终点');
            if (window.Android) {
                window.Android.onLocationSelected(lat, lng);
            }
        } else {
            var routeEnd = L.latLng(lat, lng);
            if (routeLine) map.removeLayer(routeLine);
            routeLine = L.polyline([routeStart, routeEnd], {color: 'blue', weight: 3}).addTo(map);
            L.marker([lat, lng]).addTo(map)
                .bindPopup('终点: ' + lat.toFixed(6) + ', ' + lng.toFixed(6)).openPopup();
            setInfoText('路线已设置，点击"路线模拟"开始');
            if (window.Android) {
                window.Android.onRouteSelected(routeStart.lat, routeStart.lng, lat, lng);
            }
            routeMode = false;
            routeStart = null;
        }
    } else {
        if (selectedMarker) map.removeLayer(selectedMarker);
        selectedMarker = L.marker([lat, lng]).addTo(map)
            .bindPopup('目标: ' + lat.toFixed(6) + ', ' + lng.toFixed(6))
            .openPopup();
        setInfoText('已选位置: ' + lat.toFixed(6) + ', ' + lng.toFixed(6));
        if (window.Android) {
            window.Android.onLocationSelected(lat, lng);
        }
    }
});

function setRouteMode(enabled) {
    routeMode = enabled;
    routeStart = null;
    if (routeLine) {
        map.removeLayer(routeLine);
        routeLine = null;
    }
    if (selectedMarker) {
        map.removeLayer(selectedMarker);
        selectedMarker = null;
    }
    if (enabled) {
        setInfoText('路线模式：请点击起点');
    }
}

function updateMockMarker(lat, lng) {
    if (mockMarker) {
        map.removeLayer(mockMarker);
    }
    var icon = L.divIcon({
        className: 'mock-marker',
        html: '<div style="background:#F44336;width:14px;height:14px;border-radius:50%;border:2px solid white;box-shadow:0 0 6px rgba(0,0,0,0.5);"></div>',
        iconSize: [18, 18],
        iconAnchor: [9, 9]
    });
    mockMarker = L.marker([lat, lng], {icon: icon}).addTo(map)
        .bindPopup('模拟位置: ' + lat.toFixed(6) + ', ' + lng.toFixed(6));
    map.setView([lat, lng]);
    setInfoText('模拟中: ' + lat.toFixed(6) + ', ' + lng.toFixed(6));
}

function clearMockMarker() {
    if (mockMarker) {
        map.removeLayer(mockMarker);
        mockMarker = null;
    }
    setInfoText('模拟已停止');
}

function searchLocation() {
    var query = document.getElementById('searchInput').value;
    if (!query) return;
    setInfoText('搜索中...');

    var url = 'https://nominatim.openstreetmap.org/search?format=json&q=' + encodeURIComponent(query) + '&accept-language=zh-CN';

    fetch(url)
        .then(function(response) { return response.json(); })
        .then(function(data) {
            if (data && data.length > 0) {
                var lat = parseFloat(data[0].lat);
                var lon = parseFloat(data[0].lon);
                map.setView([lat, lon], 15);
                if (selectedMarker) map.removeLayer(selectedMarker);
                selectedMarker = L.marker([lat, lon]).addTo(map)
                    .bindPopup(data[0].display_name).openPopup();
                setInfoText('已找到: ' + lat.toFixed(6) + ', ' + lon.toFixed(6));
                if (window.Android) {
                    window.Android.onLocationSelected(lat, lon);
                }
            } else {
                setInfoText('未找到该地点');
            }
        })
        .catch(function(err) {
            setInfoText('搜索失败: ' + err.message);
        });
}

document.getElementById('searchInput').addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        searchLocation();
    }
});
