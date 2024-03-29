/* Copyright (c) 2023 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

#ifndef BRAVE_BROWSER_BRAVE_WALLET_ZCASH_WALLET_SERVICE_FACTORY_H_
#define BRAVE_BROWSER_BRAVE_WALLET_ZCASH_WALLET_SERVICE_FACTORY_H_

#include "brave/components/brave_wallet/common/brave_wallet.mojom.h"
#include "components/keyed_service/content/browser_context_keyed_service_factory.h"
#include "components/keyed_service/core/keyed_service.h"
#include "content/public/browser/browser_context.h"
#include "mojo/public/cpp/bindings/pending_receiver.h"
#include "mojo/public/cpp/bindings/pending_remote.h"

namespace base {
template <typename T>
class NoDestructor;
}  // namespace base

namespace brave_wallet {

class ZCashWalletService;

class ZCashWalletServiceFactory : public BrowserContextKeyedServiceFactory {
 public:
  ZCashWalletServiceFactory(const ZCashWalletServiceFactory&) = delete;
  ZCashWalletServiceFactory& operator=(const ZCashWalletServiceFactory&) =
      delete;

  static mojo::PendingRemote<mojom::ZCashWalletService> GetForContext(
      content::BrowserContext* context);
  static ZCashWalletService* GetServiceForContext(
      content::BrowserContext* context);
  static ZCashWalletServiceFactory* GetInstance();
  static void BindForContext(
      content::BrowserContext* context,
      mojo::PendingReceiver<mojom::ZCashWalletService> receiver);

 private:
  friend base::NoDestructor<ZCashWalletServiceFactory>;

  ZCashWalletServiceFactory();
  ~ZCashWalletServiceFactory() override;

  KeyedService* BuildServiceInstanceFor(
      content::BrowserContext* context) const override;
  content::BrowserContext* GetBrowserContextToUse(
      content::BrowserContext* context) const override;
};

}  // namespace brave_wallet

#endif  // BRAVE_BROWSER_BRAVE_WALLET_ZCASH_WALLET_SERVICE_FACTORY_H_
