import { useFetch } from "@vueuse/core"

export function useConfigurationApi() {
  function list() {
    return useFetch("/api/configuration/list")
  }

  function save() {
    return useFetch("/api/configuration/save")
      .post({
        isPreventFolderRecursiveRemoval: true,
        isRunSynchronizationOnStartup: true,
        isUseTrashOverDelete: true,
      })
      .json()
  }

  return {
    list,
    save,
  }
}

export function useFolderApi() {
  function listRemote() {
    return useFetch("/api/folder/list_remote?page=0&pageSize=10")
  }

  function add() {
    return useFetch("/api/folder/add")
      .post({
        localFolder: "",
        remoteFolder: "",
      })
      .json()
  }

  function list() {
    return useFetch("/api/folder/list?page=0&pageSize=10")
  }

  function remove() {
    return useFetch("/api/folder/remove").delete({
      remoteFolder: "",
    })
  }

  function addExclusionPattern() {
    return useFetch("/api/folder/add/exclusion_pattern").post({
      remoteFolder: "",
      regexp: "",
    })
  }

  function removeExclusionPattern() {
    return useFetch("/api/folder/remove/exclusion_pattern").post({
      remoteFolder: "",
      regexp: "",
    })
  }

  return {
    listRemote,
    add,
    list,
    remove,
    addExclusionPattern,
    removeExclusionPattern,
  }
}

export function useTrashApi() {
  function navigate() {
    return useFetch("/api/trash/navigate")
  }

  function remove() {
    return useFetch("/api/trash/delete")
      .delete({
        keys: [""],
      })
      .json()
  }

  function recover() {
    return useFetch("/api/trash/recover")
      .post({
        keys: [""],
        toRemoteFolder: "",
      })
      .json()
  }

  return {
    navigate,
    remove,
    recover,
  }
}
