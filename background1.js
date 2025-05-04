chrome.runtime.onInstalled.addListener(() => {
    chrome.contextMenus.create({
      id: "addToSheet",
      title: "📋 Add to Sheet",
      contexts: ["selection"]
    });
  });
  
  chrome.contextMenus.onClicked.addListener((info, tab) => {
    if (info.menuItemId === "addToSheet" && info.selectionText) {
      // Lưu từ được chọn vào storage → popup sẽ đọc
      chrome.storage.local.set({ selectedWord: info.selectionText.trim() }, () => {
        // Mở popup
        chrome.action.openPopup();
      });
    }
  });


