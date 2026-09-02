-- Optional total spend ceiling for a campaign. NULL means unlimited (existing behavior for every
-- campaign created before this migration). See Campaign.maxBudgetInr / ContentServiceImpl.submit().
ALTER TABLE campaigns
    ADD COLUMN max_budget_inr DECIMAL(12,2) NULL AFTER rate_per_thousand_views_inr;
