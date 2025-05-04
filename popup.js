document.addEventListener("DOMContentLoaded", async () => {
  const wordInput = document.getElementById("wordInput");
  const noteInput = document.getElementById("noteInput");
  const addBtn = document.getElementById("addBtn");

  chrome.storage.local.get("selectedWord", (data) => {
    wordInput.value = data.selectedWord || "";
  });

  addBtn.addEventListener("click", async () => {
    const word = wordInput.value.trim();
    const note = noteInput.value.trim();
    if (!word) return alert("Bạn chưa nhập từ!");

    const exists = await checkIfWordExists(word);
    if (exists) {
      alert("⚠️ Từ này đã có trong Google Sheet rồi!");
      return;
    }

    try {
      console.log("📤 Bắt đầu lấy access token...");
      const token = await getAccessToken();
      console.log("✅ Access Token:", token);

      const body = {
        majorDimension: "ROWS",
        values: [[word, note]]
      };

      const url = "https://sheets.googleapis.com/v4/spreadsheets/1lapAW4hH7mso0dzMqaKCQyrNuIDTL2sfNr-nIWRmrag/values/Sheet1!A1:append?valueInputOption=USER_ENTERED";

      const res = await fetch(url, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify(body)
      });

      const result = await res.json();
      console.log("📄 Response from Sheets:", result);

      if (res.ok) {
        alert("✅ Đã ghi vào Google Sheet!");
      } else {
        alert("❌ Lỗi từ Google API: " + result.error.message);
      }
    } catch (err) {
      console.error("❌ Lỗi gọi API:", err);
      alert("❌ Lỗi gọi Google Sheets API!" + err);
    }
  });
});

// ✅ Fix: chỉ trả về `access_token` thay vì toàn bộ object
async function getAccessToken() {
  const header = { alg: "RS256", typ: "JWT" };
  const now = Math.floor(Date.now() / 1000);
  const payload = {
    iss: serviceAccount.client_email,
    scope: "https://www.googleapis.com/auth/spreadsheets",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600
  };

  const toBase64Url = (obj) => btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  const enc = new TextEncoder();
  const jwtHeader = toBase64Url(header);
  const jwtPayload = toBase64Url(payload);
  const unsignedJWT = `${jwtHeader}.${jwtPayload}`;

  const key = await importPrivateKey(serviceAccount.private_key);
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, enc.encode(unsignedJWT));
  const b64sig = btoa(String.fromCharCode(...new Uint8Array(signature))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');

  const jwt = `${unsignedJWT}.${b64sig}`;

  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`
  });

  const data = await res.json();
  if (data.access_token) {
    return data.access_token;
  } else {
    throw new Error(data.error_description || "Không lấy được token");
  }
}

async function importPrivateKey(pemKey) {
  const pem = pemKey.replace(/-----.*?-----|\n/g, '');
  const binary = atob(pem);
  const buffer = new Uint8Array([...binary].map(c => c.charCodeAt(0)));
  return await crypto.subtle.importKey(
    "pkcs8",
    buffer.buffer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );
}



async function checkIfWordExists(word) {
  const token = await getAccessToken(); // bạn đã có sẵn
  const sheetId = "1lapAW4hH7mso0dzMqaKCQyrNuIDTL2sfNr-nIWRmrag";
  const range = "Sheet1!A:A";

  const res = await fetch(`https://sheets.googleapis.com/v4/spreadsheets/${sheetId}/values/${range}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`,
    }
  });

  const data = await res.json();

  if (!res.ok) {
    console.error("❌ Lỗi lấy dữ liệu cột A:", data.error);
    return false;
  }

  const allWords = (data.values || []).flat().map(w => w.toLowerCase().trim());
  return allWords.includes(word.toLowerCase());
}
